cmake_minimum_required(VERSION 3.22.1)

project("neotools")

# ── CrashHandler: loaded FIRST by Application ────────────────────────────
# Separate library so signal handlers are installed before any other native
# code runs. Contains sigaction handlers + crash file read.
add_library(
    CrashHandler
    SHARED
    crash/CrashHandler.cpp
)

target_include_directories(
    CrashHandler
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(
    CrashHandler
    log
)

# ── neotools: main library with all tools ────────────────────────────────
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
    imageeditor/filters.cpp
    apk/apk_parser.cpp
)

find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)
find_library(z-lib z)

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
    ${z-lib}
)
