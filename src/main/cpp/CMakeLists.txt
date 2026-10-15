cmake_minimum_required(VERSION 3.20)
project(ru.axefu.ogava LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED  ON)
set(CMAKE_CXX_EXTENSIONS OFF)

find_package(JNI REQUIRED)
find_package(OpenGL REQUIRED)
find_package(GLEW REQUIRED)
find_package(glfw3 CONFIG REQUIRED)

add_library(ogava SHARED
    src/ru_axefu_ogava_Window.cpp
)

target_include_directories(ogava PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_link_libraries(ogava PRIVATE
    JNI::JNI
    OpenGL::GL
    GLEW::GLEW
    glfw
)

set_target_properties(ogava PROPERTIES
    OUTPUT_NAME "ogava"
    POSITION_INDEPENDENT_CODE ON
)

set_target_properties(ogava PROPERTIES
        LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/../../../target/generated-resources/natives"
)