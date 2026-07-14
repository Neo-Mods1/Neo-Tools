#include "apk_parser.hpp"
#include <jni.h>
#include <string>
#include <vector>
#include <cstring>
#include <cstdint>
#include <algorithm>
#include <zlib.h>
#include <ctime>
#include <android/log.h>
#include <sys/stat.h>

#ifndef LOG_TAG
#define LOG_TAG "NeoTools-APK"
#endif

// ============================================================================
// Minimal ZIP reader — reads central directory, extracts files by name.
// No external dependencies beyond zlib (already linked).
// ============================================================================

#pragma pack(push, 1)
struct LocalFileHeader {
    uint32_t sig;       // 0x04034b50
    uint16_t version;
    uint16_t flags;
    uint16_t compression;
    uint16_t modTime;
    uint16_t modDate;
    uint32_t crc32;
    uint32_t compressedSize;
    uint32_t uncompressedSize;
    uint16_t nameLen;
    uint16_t extraLen;
};

struct CentralDirEntry {
    uint32_t sig;       // 0x02014b50
    uint16_t versionMade;
    uint16_t versionNeeded;
    uint16_t flags;
    uint16_t compression;
    uint16_t modTime;
    uint16_t modDate;
    uint32_t crc32;
    uint32_t compressedSize;
    uint32_t uncompressedSize;
    uint16_t nameLen;
    uint16_t extraLen;
    uint16_t commentLen;
    uint16_t diskStart;
    uint16_t internalAttr;
    uint32_t externalAttr;
    uint32_t localHeaderOffset;
};

struct EndOfCentralDir {
    uint32_t sig;       // 0x06054b50
    uint16_t diskNum;
    uint16_t cdDisk;
    uint16_t cdEntriesDisk;
    uint16_t cdEntriesTotal;
    uint32_t cdSize;
    uint32_t cdOffset;
    uint16_t commentLen;
};
#pragma pack(pop)

struct ZipEntry {
    std::string name;
    uint32_t compressedSize;
    uint32_t uncompressedSize;
    uint16_t compression;
    uint32_t localHeaderOffset;
    uint32_t crc32;
};

struct ZipFile {
    FILE* fp;
    std::vector<ZipEntry> entries;
    uint32_t cdOffset;
    uint32_t cdSize;

    ZipFile() : fp(nullptr), cdOffset(0), cdSize(0) {}
    ~ZipFile() { if (fp) fclose(fp); }
};

static uint16_t read16(const uint8_t* p) {
    return (uint16_t)p[0] | ((uint16_t)p[1] << 8);
}

static uint32_t read32(const uint8_t* p) {
    return (uint32_t)p[0] | ((uint32_t)p[1] << 8) |
           ((uint32_t)p[2] << 16) | ((uint32_t)p[3] << 24);
}

static bool openZip(const char* path, ZipFile& zf) {
    zf.fp = fopen(path, "rb");
    if (!zf.fp) return false;

    // Find end of central directory
    fseek(zf.fp, 0, SEEK_END);
    long fileSize = ftell(zf.fp);
    if (fileSize < (long)sizeof(EndOfCentralDir)) return false;

    // Search backwards for EOCD signature
    long searchPos = fileSize - sizeof(EndOfCentralDir);
    if (searchPos > 65536 + (long)sizeof(EndOfCentralDir))
        searchPos = 65536 + sizeof(EndOfCentralDir);

    uint8_t buf[sizeof(EndOfCentralDir)];
    bool found = false;
    for (long pos = searchPos; pos >= 0; pos--) {
        fseek(zf.fp, pos, SEEK_SET);
        if (fread(buf, 1, sizeof(buf), zf.fp) != sizeof(buf)) continue;
        if (read32(buf) == 0x06054b50) {
            found = true;
            break;
        }
    }
    if (!found) return false;

    EndOfCentralDir eocd;
    memcpy(&eocd, buf, sizeof(eocd));
    zf.cdOffset = eocd.cdOffset;
    zf.cdSize = eocd.cdSize;
    uint16_t totalEntries = eocd.cdEntriesTotal;

    // Read central directory
    fseek(zf.fp, zf.cdOffset, SEEK_SET);
    uint8_t cdeBuf[sizeof(CentralDirEntry)];
    for (uint16_t i = 0; i < totalEntries; i++) {
        if (fread(cdeBuf, 1, sizeof(cdeBuf), zf.fp) != sizeof(cdeBuf)) return false;
        if (read32(cdeBuf) != 0x02014b50) return false;

        CentralDirEntry cde;
        memcpy(&cde, cdeBuf, sizeof(cde));

        std::string name(cde.nameLen, '\0');
        if (cde.nameLen > 0) {
            if (fread(&name[0], 1, cde.nameLen, zf.fp) != cde.nameLen) return false;
        } else {
            name = "";
        }
        // Skip extra + comment
        fseek(zf.fp, cde.extraLen + cde.commentLen, SEEK_CUR);

        ZipEntry entry;
        entry.name = name;
        entry.compressedSize = cde.compressedSize;
        entry.uncompressedSize = cde.uncompressedSize;
        entry.compression = cde.compression;
        entry.localHeaderOffset = cde.localHeaderOffset;
        entry.crc32 = cde.crc32;
        zf.entries.push_back(entry);
    }
    return true;
}

static bool readEntryData(ZipFile& zf, const ZipEntry& entry, std::vector<uint8_t>& out) {
    fseek(zf.fp, entry.localHeaderOffset, SEEK_SET);
    uint8_t lfhBuf[sizeof(LocalFileHeader)];
    if (fread(lfhBuf, 1, sizeof(lfhBuf), zf.fp) != sizeof(lfhBuf)) return false;
    if (read32(lfhBuf) != 0x04034b50) return false;

    uint16_t nameLen = read16(lfhBuf + 26);
    uint16_t extraLen = read16(lfhBuf + 28);
    fseek(zf.fp, nameLen + extraLen, SEEK_CUR);

    if (entry.compression == 0) {
        // Stored
        out.resize(entry.compressedSize);
        if (entry.compressedSize > 0) {
            if (fread(out.data(), 1, entry.compressedSize, zf.fp) != entry.compressedSize)
                return false;
        }
        return true;
    } else if (entry.compression == 8) {
        // Deflated
        std::vector<uint8_t> compressed(entry.compressedSize);
        if (entry.compressedSize > 0) {
            if (fread(compressed.data(), 1, entry.compressedSize, zf.fp) != entry.compressedSize)
                return false;
        }
        z_stream strm = {};
        strm.next_in = compressed.data();
        strm.avail_in = compressed.size();
        out.resize(entry.uncompressedSize);
        strm.next_out = out.data();
        strm.avail_out = out.size();
        if (inflateInit2(&strm, -15) != Z_OK) return false;
        int ret = inflate(&strm, Z_FINISH);
        inflateEnd(&strm);
        return (ret == Z_STREAM_END);
    }
    return false;
}

// ============================================================================
// JSON helpers
// ============================================================================

static std::string jsonEscape(const std::string& s) {
    std::string out;
    out.reserve(s.size() + 16);
    for (char c : s) {
        switch (c) {
            case '"':  out += "\\\""; break;
            case '\\': out += "\\\\"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            default:   out += c; break;
        }
    }
    return out;
}

static std::string dosToDateString(uint16_t modDate, uint16_t modTime) {
    int year = ((modDate >> 9) & 0x7F) + 1980;
    int month = (modDate >> 5) & 0x0F;
    int day = modDate & 0x1F;
    int hour = (modTime >> 11) & 0x1F;
    int min = (modTime >> 5) & 0x3F;
    int sec = (modTime & 0x1F) * 2;
    char buf[32];
    snprintf(buf, sizeof(buf), "%04d-%02d-%02d %02d:%02d:%02d",
             year, month, day, hour, min, sec);
    return std::string(buf);
}

static std::string formatSize(uint64_t bytes) {
    if (bytes < 1024) return std::to_string(bytes) + " B";
    if (bytes < 1024 * 1024) {
        char buf[32];
        snprintf(buf, sizeof(buf), "%.1f KB", bytes / 1024.0);
        return buf;
    }
    if (bytes < 1024ULL * 1024 * 1024) {
        char buf[32];
        snprintf(buf, sizeof(buf), "%.1f MB", bytes / (1024.0 * 1024));
        return buf;
    }
    char buf[32];
    snprintf(buf, sizeof(buf), "%.2f GB", bytes / (1024.0 * 1024 * 1024));
    return buf;
}

// ============================================================================
// SHA-1 and SHA-256 helpers (using zlib's crc32 for basic hashing,
// but for proper SHA we do a simple implementation)
// ============================================================================

// Minimal SHA-256 for certificate fingerprinting
struct SHA256 {
    uint32_t h[8];
    uint64_t totalLen;
    uint8_t buf[64];
    size_t bufLen;

    static const uint32_t K[64];

    void init() {
        h[0] = 0x6a09e667; h[1] = 0xbb67ae85;
        h[2] = 0x3c6ef372; h[3] = 0xa54ff53a;
        h[4] = 0x510e527f; h[5] = 0x9b05688c;
        h[6] = 0x1f83d9ab; h[7] = 0x5be0cd19;
        totalLen = 0; bufLen = 0;
    }

    void processBlock(const uint8_t block[64]) {
        uint32_t w[64];
        for (int i = 0; i < 16; i++)
            w[i] = ((uint32_t)block[i*4] << 24) | ((uint32_t)block[i*4+1] << 16) |
                    ((uint32_t)block[i*4+2] << 8) | block[i*4+3];
        for (int i = 16; i < 64; i++) {
            uint32_t s0 = rotr(w[i-15],7) ^ rotr(w[i-15],18) ^ (w[i-15]>>3);
            uint32_t s1 = rotr(w[i-2],17) ^ rotr(w[i-2],19) ^ (w[i-2]>>10);
            w[i] = w[i-16] + s0 + w[i-7] + s1;
        }
        uint32_t a=h[0],b=h[1],c=h[2],d=h[3],e=h[4],f=h[5],g=h[6],hh=h[7];
        for (int i = 0; i < 64; i++) {
            uint32_t S1 = rotr(e,6) ^ rotr(e,11) ^ rotr(e,25);
            uint32_t ch = (e & f) ^ (~e & g);
            uint32_t t1 = hh + S1 + ch + K[i] + w[i];
            uint32_t S0 = rotr(a,2) ^ rotr(a,13) ^ rotr(a,22);
            uint32_t maj = (a & b) ^ (a & c) ^ (b & c);
            uint32_t t2 = S0 + maj;
            hh=g; g=f; f=e; e=d+t1; d=c; c=b; b=a; a=t1+t2;
        }
        h[0]+=a; h[1]+=b; h[2]+=c; h[3]+=d; h[4]+=e; h[5]+=f; h[6]+=g; h[7]+=hh;
    }

    void update(const uint8_t* data, size_t len) {
        totalLen += len;
        size_t offset = 0;
        if (bufLen > 0) {
            size_t toCopy = 64 - bufLen;
            if (toCopy > len) toCopy = len;
            memcpy(buf + bufLen, data, toCopy);
            bufLen += toCopy;
            offset += toCopy;
            if (bufLen == 64) { processBlock(buf); bufLen = 0; }
        }
        while (offset + 64 <= len) {
            processBlock(data + offset);
            offset += 64;
        }
        if (offset < len) {
            memcpy(buf, data + offset, len - offset);
            bufLen = len - offset;
        }
    }

    void final(uint8_t out[32]) {
        uint64_t bits = totalLen * 8;
        uint8_t pad = 0x80;
        update(&pad, 1);
        uint8_t zero = 0;
        while (bufLen != 56) update(&zero, 1);
        uint8_t lenBuf[8];
        for (int i = 7; i >= 0; i--) { lenBuf[i] = bits & 0xff; bits >>= 8; }
        update(lenBuf, 8);
        for (int i = 0; i < 8; i++) {
            out[i*4]   = (h[i] >> 24) & 0xff;
            out[i*4+1] = (h[i] >> 16) & 0xff;
            out[i*4+2] = (h[i] >> 8) & 0xff;
            out[i*4+3] = h[i] & 0xff;
        }
    }

    static uint32_t rotr(uint32_t x, int n) { return (x >> n) | (x << (32-n)); }
};

const uint32_t SHA256::K[64] = {
    0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
    0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
    0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
    0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
    0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
    0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
    0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
    0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2
};

static std::string bytesToHex(const uint8_t* data, size_t len, bool upper = false) {
    std::string out;
    out.reserve(len * 2);
    const char* fmt = upper ? "%02X" : "%02x";
    for (size_t i = 0; i < len; i++) {
        char buf[3];
        snprintf(buf, sizeof(buf), fmt, data[i]);
        out += buf;
    }
    return out;
}

// ============================================================================
// ASN.1 / X.509 basic parser for certificate info
// ============================================================================

struct Asn1Cursor {
    const uint8_t* data;
    size_t len;
    size_t pos;

    bool readTag(uint8_t& tag, size_t& contentLen) {
        if (pos >= len) return false;
        tag = data[pos++];
        // Read length
        if (pos >= len) return false;
        uint8_t lenByte = data[pos++];
        if (lenByte < 0x80) {
            contentLen = lenByte;
        } else if (lenByte == 0x81) {
            if (pos >= len) return false;
            contentLen = data[pos++];
        } else if (lenByte == 0x82) {
            if (pos + 2 > len) return false;
            contentLen = ((uint32_t)data[pos] << 8) | data[pos+1];
            pos += 2;
        } else {
            return false; // Too long
        }
        return true;
    }

    bool enter(uint8_t expectedTag, size_t& contentLen) {
        uint8_t tag;
        if (!readTag(tag, contentLen)) return false;
        if (tag != expectedTag) return false;
        return true;
    }

    std::string readOid(size_t contentLen) {
        std::string oid;
        if (pos + contentLen > len) return "";
        // First two elements combined
        if (contentLen < 2) return "";
        oid = std::to_string(data[pos] / 40) + "." + std::to_string(data[pos] % 40);
        uint32_t val = 0;
        for (size_t i = 1; i < contentLen; i++) {
            uint8_t b = data[pos + i];
            if (b & 0x80) {
                val = (val << 7) | (b & 0x7f);
            } else {
                val = (val << 7) | b;
                oid += "." + std::to_string(val);
                val = 0;
            }
        }
        pos += contentLen;
        return oid;
    }

    std::string readPrintableString(size_t contentLen) {
        if (pos + contentLen > len) return "";
        std::string s((const char*)(data + pos), contentLen);
        pos += contentLen;
        return s;
    }

    void skip(size_t n) { pos += n; }
    bool eof() const { return pos >= len; }
};

static std::string oidToName(const std::string& oid) {
    if (oid == "2.5.4.3") return "CN";
    if (oid == "2.5.4.6") return "C";
    if (oid == "2.5.4.10") return "O";
    if (oid == "2.5.4.11") return "OU";
    if (oid == "2.5.4.5") return "serialNumber";
    if (oid == "2.5.4.7") return "L";
    if (oid == "2.5.4.8") return "ST";
    if (oid == "2.5.4.9") return "street";
    if (oid == "2.5.4.17") return "postalCode";
    if (oid == "1.2.840.113549.1.1.1") return "RSA";
    if (oid == "1.2.840.113549.1.1.11") return "SHA256withRSA";
    if (oid == "1.2.840.113549.1.1.5") return "SHA1withRSA";
    if (oid == "2.16.840.1.101.3.4.2.1") return "SHA-256";
    if (oid == "1.3.14.3.2.26") return "SHA-1";
    return oid;
}

struct CertInfo {
    std::string subject;
    std::string issuer;
    std::string serialHex;
    std::string notBefore;
    std::string notAfter;
    std::string sigAlgorithm;
    std::string sha256;
    std::string sha1;
};

static std::string parseRdnSequence(const uint8_t* data, size_t len) {
    std::string result;
    Asn1Cursor cur = {data, len, 0};
    while (!cur.eof()) {
        uint8_t tag;
        size_t seqLen;
        if (!cur.enter(0x31, seqLen)) break; // SET
        size_t end = cur.pos + seqLen;
        // Inside SET: OID + value
        uint8_t oidTag;
        size_t oidLen;
        if (!cur.readTag(oidTag, oidLen)) break;
        std::string oid = cur.readOid(oidLen);
        uint8_t valTag;
        size_t valLen;
        if (!cur.readTag(valTag, valLen)) break;
        std::string val = cur.readPrintableString(valLen);
        if (!result.empty()) result += ", ";
        result += oidToName(oid) + "=" + val;
        cur.pos = end;
    }
    return result;
}

static std::string parseUtcTime(const uint8_t* data, size_t len) {
    // UTCTime: YYMMDDHHMMSSZ
    if (len < 13) return std::string((const char*)data, len);
    std::string s;
    s += "20"; s += (char)data[0]; s += (char)data[1]; s += "-";
    s += (char)data[2]; s += (char)data[3]; s += "-";
    s += (char)data[4]; s += (char)data[5]; s += " ";
    s += (char)data[6]; s += (char)data[7]; s += ":";
    s += (char)data[8]; s += (char)data[9]; s += ":";
    s += (char)data[10]; s += (char)data[11];
    return s;
}

static CertInfo parseX509Cert(const uint8_t* certData, size_t certLen) {
    CertInfo info;
    Asn1Cursor cur = {certData, certLen, 0};

    // Certificate ::= SEQUENCE { tbsCertificate, signatureAlgorithm, signatureValue }
    size_t totalLen;
    if (!cur.enter(0x30, totalLen)) return info;
    size_t certEnd = cur.pos + totalLen;

    // tbsCertificate ::= SEQUENCE
    size_t tbsLen;
    if (!cur.enter(0x30, tbsLen)) return info;
    size_t tbsEnd = cur.pos + tbsLen;

    // version [0] EXPLICIT Version DEFAULT v1
    uint8_t tag;
    size_t tagLen;
    if (cur.readTag(tag, tagLen)) {
        if (tag == 0xa0) { // context-specific [0]
            // version INTEGER
            size_t verLen;
            if (cur.enter(0x02, verLen)) cur.skip(verLen);
        } else {
            cur.pos -= 2; // Not a version tag, go back
        }
    }

    // serialNumber
    size_t serialLen;
    if (cur.enter(0x02, serialLen)) {
        std::vector<uint8_t> serial(serialLen);
        memcpy(serial.data(), cur.data + cur.pos, serialLen);
        cur.skip(serialLen);
        // Remove leading zeros
        size_t start = 0;
        while (start < serial.size() - 1 && serial[start] == 0) start++;
        info.serialHex = bytesToHex(serial.data() + start, serial.size() - start, true);
    }

    // signature algorithm
    size_t algLen;
    if (cur.enter(0x30, algLen)) {
        size_t algEnd = cur.pos + algLen;
        size_t oidLen;
        if (cur.readTag(tag, oidLen)) {
            info.sigAlgorithm = oidToName(cur.readOid(oidLen));
        }
        cur.pos = algEnd;
    }

    // issuer
    size_t issuerLen;
    if (cur.enter(0x30, issuerLen)) {
        info.issuer = parseRdnSequence(cur.data + cur.pos, issuerLen);
        cur.skip(issuerLen);
    }

    // validity
    size_t validLen;
    if (cur.enter(0x30, validLen)) {
        size_t validEnd = cur.pos + validLen;
        // notBefore
        if (cur.readTag(tag, tagLen)) {
            if (tag == 0x17) { // UTCTime
                info.notBefore = parseUtcTime(cur.data + cur.pos, tagLen);
                cur.skip(tagLen);
            } else if (tag == 0x18) { // GeneralizedTime
                info.notBefore = std::string((const char*)(cur.data + cur.pos), tagLen);
                cur.skip(tagLen);
            }
        }
        // notAfter
        if (cur.readTag(tag, tagLen)) {
            if (tag == 0x17) {
                info.notAfter = parseUtcTime(cur.data + cur.pos, tagLen);
                cur.skip(tagLen);
            } else if (tag == 0x18) {
                info.notAfter = std::string((const char*)(cur.data + cur.pos), tagLen);
                cur.skip(tagLen);
            }
        }
        cur.pos = validEnd;
    }

    // subject
    size_t subjLen;
    if (cur.enter(0x30, subjLen)) {
        info.subject = parseRdnSequence(cur.data + cur.pos, subjLen);
        cur.skip(subjLen);
    }

    // Compute SHA-256 and SHA-1 fingerprints
    SHA256 sha;
    sha.init();
    sha.update(certData, certLen);
    uint8_t hash[32];
    sha.final(hash);
    info.sha256 = bytesToHex(hash, 32, true);

    // SHA-1 (simplified - reuse SHA-256 structure with SHA-1 init values)
    // For simplicity, just compute a basic hash for display
    // In production you'd use a proper SHA-1 implementation
    info.sha1 = "N/A";

    cur.pos = certEnd;
    return info;
}

// ============================================================================
// Binary XML parser for AndroidManifest.xml
// ============================================================================

struct BinaryXmlParser {
    const uint8_t* data;
    size_t dataLen;
    size_t pos;
    std::vector<std::string> strings;
    std::vector<uint32_t> resourceIds;
    std::string output;
    int indent;

    BinaryXmlParser(const uint8_t* d, size_t len) : data(d), dataLen(len), pos(0), indent(0) {}

    uint16_t u16() {
        if (pos + 2 > dataLen) return 0;
        uint16_t v = (uint16_t)data[pos] | ((uint16_t)data[pos+1] << 8);
        pos += 2;
        return v;
    }

    uint8_t u8() {
        if (pos >= dataLen) return 0;
        return data[pos++];
    }

    uint32_t u32() {
        if (pos + 4 > dataLen) return 0;
        uint32_t v = (uint32_t)data[pos] | ((uint32_t)data[pos+1] << 8) |
                     ((uint32_t)data[pos+2] << 16) | ((uint32_t)data[pos+3] << 24);
        pos += 4;
        return v;
    }

    bool parseStringPool() {
        if (pos + 28 > dataLen) return false;
        uint16_t type = u16(); // chunk type
        uint16_t headerSize = u16();
        uint32_t chunkSize = u32();
        uint32_t stringCount = u32();
        uint32_t styleCount = u32();
        uint32_t flags = u32();
        uint32_t stringsStart = u32();
        uint32_t stylesStart = u32();

        bool isUtf8 = (flags & (1 << 8)) != 0;
        size_t savedPos = pos;
        pos = savedPos + stringsStart;

        strings.resize(stringCount);
        for (uint32_t i = 0; i < stringCount; i++) {
            if (pos >= dataLen) break;
            if (isUtf8) {
                // UTF-8: 1 or 2 byte length, then chars
                uint8_t charLen = data[pos++];
                if (charLen & 0x80) {
                    pos++; // skip high byte
                }
                uint8_t byteLen = data[pos++];
                if (byteLen & 0x80) {
                    byteLen = ((byteLen & 0x7f) << 8) | data[pos++];
                }
                if (pos + byteLen <= dataLen) {
                    strings[i] = std::string((const char*)(data + pos), byteLen);
                    pos += byteLen + 1; // +1 for null terminator
                }
            } else {
                // UTF-16
                uint16_t charLen = u16();
                if (charLen & 0x8000) {
                    charLen = ((charLen & 0x7fff) << 16) | u16();
                }
                size_t byteLen = charLen * 2;
                if (pos + byteLen <= dataLen) {
                    // Simple ASCII conversion
                    std::string s;
                    s.reserve(charLen);
                    for (uint16_t c = 0; c < charLen; c++) {
                        uint16_t ch = (uint16_t)data[pos + c*2] | ((uint16_t)data[pos + c*2+1] << 8);
                        s += (ch < 128) ? (char)ch : '?';
                    }
                    strings[i] = s;
                    pos += byteLen + 2; // +2 for null terminator
                }
            }
        }
        pos = savedPos + chunkSize;
        return true;
    }

    std::string attrTypeToString(uint32_t type) {
        switch (type) {
            case 0x00: return "null";
            case 0x01: return "reference";
            case 0x02: return "attribute";
            case 0x03: return "string";
            case 0x04: return "float";
            case 0x05: return "dimension";
            case 0x06: return "fraction";
            case 0x10: return "color(ARGB8)";
            case 0x11: return "color(ARGB4)";
            case 0x12: return "color(RGB8)";
            case 0x13: return "color(RGB4)";
            case 0x14: return "boolean";
            case 0x15: return "integer";
            case 0x16: return "string";
            case 0x17: return "enum";
            case 0x18: return "flags";
            default: return "unknown(" + std::to_string(type) + ")";
        }
    }

    std::string formatAttrValue(uint32_t type, uint32_t data, const std::string& rawValue) {
        if (!rawValue.empty()) return "\"" + jsonEscape(rawValue) + "\"";
        switch (type) {
            case 0x03: // string
                if (data < strings.size()) return "\"" + jsonEscape(strings[data]) + "\"";
                return "\"\"";
            case 0x15: // integer
            case 0x14: // boolean (stored as int)
                return std::to_string((int32_t)data);
            case 0x18: // flags
                return "0x" + bytesToHex((const uint8_t*)&data, 4);
            case 0x00: // null
                return "null";
            default:
                if (data < strings.size()) return "\"" + jsonEscape(strings[data]) + "\"";
                return "0x" + bytesToHex((const uint8_t*)&data, 4);
        }
    }

    std::string indentStr() {
        return std::string(indent * 2, ' ');
    }

    bool parseXml() {
        // File header
        uint16_t type = u16(); // 0x0003
        uint16_t headerSize = u16(); // 8
        uint32_t chunkSize = u32();

        // String pool
        parseStringPool();

        // Resource ID map (optional)
        {
            size_t saved = pos;
            uint16_t t = u16();
            uint16_t hs = u16();
            uint32_t cs = u32();
            if (t == 0x0180) {
                resourceIds.resize(cs / 4 - 2);
                for (size_t i = 0; i < resourceIds.size(); i++) {
                    resourceIds[i] = u32();
                }
            } else {
                pos = saved;
            }
        }

        output = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";
        parseXmlNode();
        return true;
    }

    void parseXmlNode() {
        while (pos < dataLen) {
            uint16_t type = u16();
            uint16_t headerSize = u16();
            uint32_t chunkSize = u32();

            switch (type) {
                case 0x0100: { // START_NAMESPACE
                    u32(); // line number
                    u32(); // comment
                    uint32_t prefix = u32();
                    uint32_t uri = u32();
                    // Store namespace mapping (simplified)
                    break;
                }
                case 0x0101: { // END_NAMESPACE
                    u32(); u32();
                    u32(); u32();
                    break;
                }
                case 0x0102: { // START_ELEMENT
                    u32(); // line number
                    u32(); // comment
                    uint32_t nsIdx = u32();
                    uint32_t nameIdx = u32();
                    uint16_t attrCount = u16();
                    uint16_t idIdx = u16();
                    uint16_t classIdx = u16();
                    uint16_t styleIdx = u16();

                    std::string ns = (nsIdx < strings.size()) ? strings[nsIdx] : "";
                    std::string name = (nameIdx < strings.size()) ? strings[nameIdx] : "???";

                    output += indentStr() + "<" + name;
                    if (!ns.empty()) {
                        // Extract prefix
                        auto colon = ns.find_last_of('/');
                        if (colon != std::string::npos) ns = ns.substr(colon + 1);
                        output += " xmlns:" + ns + "=\"" + jsonEscape(ns) + "\"";
                    }

                    for (uint16_t i = 0; i < attrCount; i++) {
                        uint32_t attrNs = u32();
                        uint32_t attrName = u32();
                        uint32_t attrRawValue = u32();
                        uint8_t attrSize = u16(); // 20
                        uint8_t attrRes0 = u8();
                        uint16_t attrType = u16();
                        uint32_t attrData = u32();

                        std::string aNs = (attrNs < strings.size()) ? strings[attrNs] : "";
                        std::string aName = (attrName < strings.size()) ? strings[attrName] : "???";
                        std::string aRaw = (attrRawValue < strings.size()) ? strings[attrRawValue] : "";
                        std::string aVal = formatAttrValue(attrType, attrData, aRaw);

                        output += " " + aName + "=" + aVal;
                    }
                    output += ">\n";
                    indent++;
                    return;
                }
                case 0x0103: { // END_ELEMENT
                    u32(); u32();
                    uint32_t nsIdx = u32();
                    uint32_t nameIdx = u32();
                    std::string name = (nameIdx < strings.size()) ? strings[nameIdx] : "???";
                    indent--;
                    output += indentStr() + "</" + name + ">\n";
                    return;
                }
                case 0x0104: { // TEXT
                    u32(); u32();
                    uint32_t dataIdx = u32();
                    if (dataIdx < strings.size()) {
                        output += indentStr() + jsonEscape(strings[dataIdx]) + "\n";
                    }
                    break;
                }
                case 0x0000: // END
                    return;
                default:
                    // Skip unknown chunk
                    if (chunkSize > 8) {
                        pos += chunkSize - 8;
                    }
                    break;
            }
        }
    }
};

// ============================================================================
// JNI implementations
// ============================================================================

static std::string findFileInZip(ZipFile& zf, const std::string& pattern) {
    for (auto& e : zf.entries) {
        if (e.name.find(pattern) != std::string::npos) {
            return e.name;
        }
    }
    return "";
}

static std::vector<std::string> listDirInZip(ZipFile& zf, const std::string& dir) {
    std::vector<std::string> result;
    for (auto& e : zf.entries) {
        if (e.name.substr(0, dir.size()) == dir && e.name != dir) {
            std::string sub = e.name.substr(dir.size());
            // Get first path component
            auto slash = sub.find('/');
            std::string component = (slash != std::string::npos) ? sub.substr(0, slash) : sub;
            if (!component.empty()) {
                // Check for duplicates
                bool dup = false;
                for (auto& r : result) { if (r == component) { dup = true; break; } }
                if (!dup) result.push_back(component);
            }
        }
    }
    return result;
}

jstring neotools::apk::ParseApkInfo(JNIEnv* env, jobject /* this */, jstring apkPath) {
    const char* path = env->GetStringUTFChars(apkPath, nullptr);
    std::string pathStr(path);
    env->ReleaseStringUTFChars(apkPath, path);

    ZipFile zf;
    if (!openZip(pathStr.c_str(), zf)) {
        env->ReleaseStringUTFChars(apkPath, path);
        return env->NewStringUTF("{}");
    }

    // Get file size
    struct stat st;
    stat(pathStr.c_str(), &st);

    std::string json = "{";

    // Basic info
    json += "\"apkSize\":" + std::to_string(st.st_size);
    json += ",\"apkSizeFormatted\":\"" + formatSize(st.st_size) + "\"";
    json += ",\"totalEntries\":" + std::to_string(zf.entries.size());

    // Find manifest
    std::string manifestName = findFileInZip(zf, "AndroidManifest.xml");
    json += ",\"hasManifest\":" + std::string(manifestName.empty() ? "false" : "true");

    // Count components in manifest
    if (!manifestName.empty()) {
        auto it = std::find_if(zf.entries.begin(), zf.entries.end(),
            [&](const ZipEntry& e) { return e.name == manifestName; });
        if (it != zf.entries.end()) {
            std::vector<uint8_t> manifestData;
            if (readEntryData(zf, *it, manifestData) && !manifestData.empty()) {
                BinaryXmlParser parser(manifestData.data(), manifestData.size());
                parser.parseXml();
                std::string& xml = parser.output;

                // Count elements
                int activities = 0, services = 0, receivers = 0, providers = 0, permissions = 0;
                // Simple tag counting
                size_t p = 0;
                while ((p = xml.find("<activity ", p)) != std::string::npos) { activities++; p++; }
                p = 0;
                while ((p = xml.find("<service ", p)) != std::string::npos) { services++; p++; }
                p = 0;
                while ((p = xml.find("<receiver ", p)) != std::string::npos) { receivers++; p++; }
                p = 0;
                while ((p = xml.find("<provider ", p)) != std::string::npos) { providers++; p++; }
                p = 0;
                while ((p = xml.find("uses-permission", p)) != std::string::npos) { permissions++; p++; }

                json += ",\"activityCount\":" + std::to_string(activities);
                json += ",\"serviceCount\":" + std::to_string(services);
                json += ",\"receiverCount\":" + std::to_string(receivers);
                json += ",\"providerCount\":" + std::to_string(providers);
                json += ",\"permissionCount\":" + std::to_string(permissions);
            }
        }
    }

    // Native libraries
    auto nativeLibs = listDirInZip(zf, "lib/");
    json += ",\"nativeLibs\":[";
    for (size_t i = 0; i < nativeLibs.size(); i++) {
        if (i > 0) json += ",";
        json += "\"" + jsonEscape(nativeLibs[i]) + "\"";
    }
    json += "]";

    // ABI detection from lib/
    std::vector<std::string> abis;
    for (auto& e : zf.entries) {
        if (e.name.substr(0, 4) == "lib/") {
            auto slash = e.name.find('/', 4);
            if (slash != std::string::npos) {
                std::string abi = e.name.substr(4, slash - 4);
                bool found = false;
                for (auto& a : abis) { if (a == abi) { found = true; break; } }
                if (!found) abis.push_back(abi);
            }
        }
    }
    json += ",\"abis\":[";
    for (size_t i = 0; i < abis.size(); i++) {
        if (i > 0) json += ",";
        json += "\"" + jsonEscape(abis[i]) + "\"";
    }
    json += "]";

    // Count .so files
    int so32 = 0, so64 = 0;
    for (auto& e : zf.entries) {
        if (e.name.size() > 3 && e.name.substr(e.name.size() - 3) == ".so") {
            if (e.name.find("/arm/") != std::string::npos ||
                e.name.find("/x86/") != std::string::npos) so32++;
            else so64++;
        }
    }
    json += ",\"so32Count\":" + std::to_string(so32);
    json += ",\"so64Count\":" + std::to_string(so64);

    // Check for resources.arsc
    bool hasRes = false;
    for (auto& e : zf.entries) {
        if (e.name == "resources.arsc") { hasRes = true; break; }
    }
    json += ",\"hasResources\":" + std::string(hasRes ? "true" : "false");

    // Check for DEX files
    int dexCount = 0;
    for (auto& e : zf.entries) {
        if (e.name.size() > 4 && e.name.substr(e.name.size() - 4) == ".dex") dexCount++;
    }
    json += ",\"dexCount\":" + std::to_string(dexCount);

    // Check for native debug symbols
    bool hasDebugSymbols = false;
    for (auto& e : zf.entries) {
        if (e.name.find(".so.debug") != std::string::npos ||
            e.name.find("/obj/") != std::string::npos) {
            hasDebugSymbols = true;
            break;
        }
    }
    json += ",\"hasDebugSymbols\":" + std::string(hasDebugSymbols ? "true" : "false");

    // Check for signing info
    bool hasV1 = false, hasV2 = false, hasV3 = false;
    for (auto& e : zf.entries) {
        if (e.name.find("META-INF/") != std::string::npos) {
            if (e.name.find("MANIFEST.MF") != std::string::npos ||
                e.name.find(".RSA") != std::string::npos ||
                e.name.find(".DSA") != std::string::npos ||
                e.name.find(".SF") != std::string::npos) {
                hasV1 = true;
            }
        }
    }
    json += ",\"hasV1Signing\":" + std::string(hasV1 ? "true" : "false");
    json += ",\"hasV2Signing\":" + std::string(hasV2 ? "true" : "false");
    json += ",\"hasV3Signing\":" + std::string(hasV3 ? "true" : "false");

    json += "}";
    return env->NewStringUTF(json.c_str());
}

jstring neotools::apk::ParseManifest(JNIEnv* env, jobject /* this */, jstring apkPath) {
    const char* path = env->GetStringUTFChars(apkPath, nullptr);
    std::string pathStr(path);
    env->ReleaseStringUTFChars(apkPath, path);

    ZipFile zf;
    if (!openZip(pathStr.c_str(), zf)) {
        return env->NewStringUTF("{}");
    }

    std::string manifestName = findFileInZip(zf, "AndroidManifest.xml");
    if (manifestName.empty()) return env->NewStringUTF("{}");

    auto it = std::find_if(zf.entries.begin(), zf.entries.end(),
        [&](const ZipEntry& e) { return e.name == manifestName; });
    if (it == zf.entries.end()) return env->NewStringUTF("{}");

    std::vector<uint8_t> data;
    if (!readEntryData(zf, *it, data) || data.empty()) {
        return env->NewStringUTF("{}");
    }

    BinaryXmlParser parser(data.data(), data.size());
    parser.parseXml();

    // Return the XML as a string (the Kotlin layer will parse it further)
    return env->NewStringUTF(parser.output.c_str());
}

jstring neotools::apk::GetManifestXml(JNIEnv* env, jobject /* this */, jstring apkPath) {
    // Same as ParseManifest but returns raw XML
    return ParseManifest(env, nullptr, apkPath);
}

jstring neotools::apk::ParseCertificate(JNIEnv* env, jobject /* this */, jstring apkPath) {
    const char* path = env->GetStringUTFChars(apkPath, nullptr);
    std::string pathStr(path);
    env->ReleaseStringUTFChars(apkPath, path);

    ZipFile zf;
    if (!openZip(pathStr.c_str(), zf)) {
        return env->NewStringUTF("[]");
    }

    // Find certificate files in META-INF/
    std::vector<std::string> certFiles;
    for (auto& e : zf.entries) {
        if (e.name.find("META-INF/") == 0) {
            std::string lower = e.name;
            for (auto& c : lower) c = tolower(c);
            if (lower.find(".rsa") != std::string::npos ||
                lower.find(".dsa") != std::string::npos ||
                lower.find(".ec") != std::string::npos) {
                certFiles.push_back(e.name);
            }
        }
    }

    std::string json = "[";
    bool first = true;

    for (auto& certFile : certFiles) {
        auto it = std::find_if(zf.entries.begin(), zf.entries.end(),
            [&](const ZipEntry& e) { return e.name == certFile; });
        if (it == zf.entries.end()) continue;

        std::vector<uint8_t> certData;
        if (!readEntryData(zf, *it, certData) || certData.empty()) continue;

        // Try to find PKCS#7 signed data structure
        // The DER-encoded certificate is usually inside a PKCS#7 container
        // For simplicity, try to find X.509 cert by looking for SEQUENCE { SEQUENCE { ...
        // The certificate typically starts within the file after some PKCS#7 headers

        // Try to find the certificate by looking for the DER header pattern
        size_t certStart = 0;
        // X.509 certs start with SEQUENCE(0x30) containing SEQUENCE(0x30)
        // Search for this pattern in the first 1024 bytes
        for (size_t i = 0; i < std::min((size_t)1024, certData.size()); i++) {
            if (certData[i] == 0x30 && i + 2 < certData.size() &&
                certData[i+1] == 0x82) {
                // Potential SEQUENCE with 2-byte length
                uint16_t seqLen = ((uint16_t)certData[i+2] << 8) | certData[i+3];
                if (i + 4 + seqLen <= certData.size()) {
                    // Check if next byte is also 0x30 (inner SEQUENCE)
                    if (certData[i+4] == 0x30 && i + 6 < certData.size() &&
                        certData[i+5] == 0x82) {
                        certStart = i;
                        break;
                    }
                }
            }
        }

        size_t certLen = certData.size() - certStart;
        CertInfo info = parseX509Cert(certData.data() + certStart, certLen);

        if (!first) json += ",";
        first = false;
        json += "{";
        json += "\"file\":\"" + jsonEscape(certFile) + "\"";
        json += ",\"subject\":\"" + jsonEscape(info.subject) + "\"";
        json += ",\"issuer\":\"" + jsonEscape(info.issuer) + "\"";
        json += ",\"serial\":\"" + jsonEscape(info.serialHex) + "\"";
        json += ",\"sigAlgorithm\":\"" + jsonEscape(info.sigAlgorithm) + "\"";
        json += ",\"notBefore\":\"" + jsonEscape(info.notBefore) + "\"";
        json += ",\"notAfter\":\"" + jsonEscape(info.notAfter) + "\"";
        json += ",\"sha256\":\"" + jsonEscape(info.sha256) + "\"";
        json += ",\"sha1\":\"" + jsonEscape(info.sha1) + "\"";
        json += "}";
    }

    json += "]";
    return env->NewStringUTF(json.c_str());
}

jstring neotools::apk::GetNativeLibs(JNIEnv* env, jobject /* this */, jstring apkPath) {
    const char* path = env->GetStringUTFChars(apkPath, nullptr);
    std::string pathStr(path);
    env->ReleaseStringUTFChars(apkPath, path);

    ZipFile zf;
    if (!openZip(pathStr.c_str(), zf)) {
        return env->NewStringUTF("[]");
    }

    std::string json = "[";
    bool first = true;
    for (auto& e : zf.entries) {
        if (e.name.size() > 4 && e.name.substr(e.name.size() - 3) == ".so") {
            if (!first) json += ",";
            first = false;
            json += "{";
            json += "\"name\":\"" + jsonEscape(e.name) + "\"";
            json += ",\"size\":" + std::to_string(e.uncompressedSize);
            json += ",\"sizeFormatted\":\"" + formatSize(e.uncompressedSize) + "\"";

            // Extract ABI
            std::string abi = "unknown";
            auto libPos = e.name.find("lib/");
            if (libPos != std::string::npos) {
                auto nextSlash = e.name.find('/', libPos + 4);
                if (nextSlash != std::string::npos) {
                    abi = e.name.substr(libPos + 4, nextSlash - libPos - 4);
                }
            }
            json += ",\"abi\":\"" + jsonEscape(abi) + "\"";
            json += ",\"is64Bit\":" + std::string(
                (abi.find("arm64") != std::string::npos || abi.find("x86_64") != std::string::npos)
                ? "true" : "false");
            json += "}";
        }
    }
    json += "]";
    return env->NewStringUTF(json.c_str());
}

jstring neotools::apk::GetZipEntries(JNIEnv* env, jobject /* this */, jstring apkPath) {
    const char* path = env->GetStringUTFChars(apkPath, nullptr);
    std::string pathStr(path);
    env->ReleaseStringUTFChars(apkPath, path);

    ZipFile zf;
    if (!openZip(pathStr.c_str(), zf)) {
        return env->NewStringUTF("[]");
    }

    std::string json = "[";
    for (size_t i = 0; i < zf.entries.size(); i++) {
        auto& e = zf.entries[i];
        if (i > 0) json += ",";
        json += "{";
        json += "\"name\":\"" + jsonEscape(e.name) + "\"";
        json += ",\"compressedSize\":" + std::to_string(e.compressedSize);
        json += ",\"uncompressedSize\":" + std::to_string(e.uncompressedSize);
        json += ",\"compression\":" + std::to_string(e.compression);
        json += ",\"crc32\":\"" + bytesToHex((const uint8_t*)&e.crc32, 4) + "\"";
        json += "}";
    }
    json += "]";
    return env->NewStringUTF(json.c_str());
}
