package com.neomods.tools.data

import com.neomods.tools.R
import com.neomods.tools.model.Tool

/**
 * Provides the tools contained in each [com.neomods.tools.model.Category].
 *
 * Every tool is a placeholder for now, but the catalogue is already structured
 * so each [Tool.id] can later be mapped to a concrete feature implementation.
 */
interface ToolRepository {

    /** All tools belonging to the category with [categoryId]. */
    fun getTools(categoryId: String): List<Tool>

    /** Lookup a single tool by [id] across all categories, or null. */
    fun getTool(id: String): Tool?

    /** Number of tools inside the category with [categoryId]. */
    fun getToolCount(categoryId: String): Int

    /** Real-time search across every tool's title and description. */
    fun searchTools(query: String): List<Tool>
}

internal class DefaultToolRepository : ToolRepository {

    private val toolsByCategory: Map<String, List<Tool>> = mapOf(
        "apk" to listOf(
            Tool("apk_info", "apk", "APK Info", "Package metadata and permissions", R.drawable.ic_tool_info, "Analysis"),
            Tool("apk_cert", "apk", "Certificate Viewer", "Inspect signing certificates", R.drawable.ic_tool_certificate, "Analysis"),
            Tool("apk_dex", "apk", "Dex Editor", "View and edit Dalvik bytecode", R.drawable.ic_tool_dex, "Editing"),
            Tool("apk_manifest", "apk", "Manifest Viewer", "Inspect the AndroidManifest", R.drawable.ic_tool_manifest, "Editing"),
            Tool("apk_manifest_edit", "apk", "Manifest Editor", "Edit manifest entries", R.drawable.ic_tool_manifest_edit, "Editing"),
            Tool("apk_smali", "apk", "Smali Viewer", "Browse disassembled smali", R.drawable.ic_tool_smali, "Editing"),
            Tool("apk_resources", "apk", "Resource Browser", "Explore packaged resources", R.drawable.ic_tool_resources, "Editing"),
            Tool("apk_sign", "apk", "APK Signer", "Sign an APK with a key", R.drawable.ic_tool_sign, "Build"),
            Tool("apk_zipalign", "apk", "ZipAlign", "Align an APK for performance", R.drawable.ic_tool_zip, "Build"),
            Tool("apk_build", "apk", "Build APK", "Assemble a package from sources", R.drawable.ic_tool_build, "Build")
        ),
        "binary" to listOf(),
        "image" to listOf(
            Tool("img_editor", "image", "Image Editor", "Professional image editing with crop, draw, text, stickers and clone", R.drawable.ic_tool_image_edit, "Editor"),
            Tool("bg_remover", "image", "BG Remover", "Remove backgrounds from images using AI", R.drawable.ic_tool_image_edit, "AI")
        ),
        "xml" to listOf(),
        "encoding" to listOf(
            Tool("enc_base64", "encoding", "Base64 Encoder", "Encode images and files to Base64", R.drawable.ic_cat_binary, "Base64"),
            Tool("dec_base64", "encoding", "Base64 Decoder", "Decode Base64 back to files and text", R.drawable.ic_cat_binary, "Base64"),
            Tool("enc_cpp_header", "encoding", "Header Generator", "Convert files to C++ header (.hpp)", R.drawable.ic_tool_hex, "C++ Headers"),
            Tool("dec_cpp_header", "encoding", "Header Decoder", "Extract original file from .hpp header", R.drawable.ic_tool_hex, "C++ Headers")
        )
    )

    private val allTools = toolsByCategory.values.flatten()
    private val byId = allTools.associateBy { it.id }

    override fun getTools(categoryId: String): List<Tool> =
        toolsByCategory[categoryId].orEmpty()

    override fun getTool(id: String): Tool? = byId[id]

    override fun getToolCount(categoryId: String): Int =
        toolsByCategory[categoryId].orEmpty().size

    override fun searchTools(query: String): List<Tool> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return allTools.filter {
            it.title.lowercase().contains(q) || it.description.lowercase().contains(q)
        }
    }
}
