cmake_minimum_required(VERSION 3.22.1)

project("neotools")

# Each tool category lives in its own folder and is added here. The root
# Main.cpp includes the per-category headers and registers their natives.
add_library(
        neotools
        SHARED
        Main.cpp
        encoding/base64.cpp
        encoding/cpp_converter.cpp
        imageeditor/adjustments.cpp
        imageeditor/crop.cpp
        imageeditor/drawing.cpp
        imageeditor/bgremove.cpp
        imageeditor/shapes.cpp
)

find_library(
        log-lib
        log
)

find_library(
        jnigraphics-lib
        jnigraphics
)

target_include_directories(
        neotools
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/Includes
        ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(
        neotools
        ${log-lib}
        ${jnigraphics-lib}
)
