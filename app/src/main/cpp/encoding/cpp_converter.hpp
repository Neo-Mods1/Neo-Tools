#pragma once

#include <jni.h>

/**
 * Encryption & Decryption -> C++ Header Generator/Decoder.
 *
 * Converts arbitrary binary files to C++ header (.hpp) files containing the
 * data as a constexpr unsigned char array, matching xxd -i format. The
 * decoder reconstructs the original file from the header.
 */
namespace neotools {
namespace encoding {

/**
 * Convert raw file bytes to a C++ header string (.hpp).
 * Format matches xxd -i: 16 bytes per line, 0x hex notation, with
 * #pragma once, constexpr array, file_size, and file_name.
 *
 * @param env      JNI environment.
 * @param thiz     the NeoNative instance (unused).
 * @param data     raw file bytes.
 * @param filename original file name (e.g. "image.png").
 * @return the complete .hpp header text as a jstring.
 */
jstring FileToHeader(JNIEnv* env, jobject thiz, jbyteArray data, jstring filename);

/**
 * Parse a .hpp header and extract the raw byte data.
 *
 * @param env    JNI environment.
 * @param thiz   the NeoNative instance (unused).
 * @param header the .hpp header text.
 * @return the decoded raw bytes as a jbyteArray.
 */
jbyteArray HeaderToFile(JNIEnv* env, jobject thiz, jstring header);

/**
 * Extract the file name from a .hpp header.
 *
 * @param env    JNI environment.
 * @param thiz   the NeoNative instance (unused).
 * @param header the .hpp header text.
 * @return the file_name value from the header as a jstring.
 */
jstring HeaderFileName(JNIEnv* env, jobject thiz, jstring header);

} // namespace encoding
} // namespace neotools
