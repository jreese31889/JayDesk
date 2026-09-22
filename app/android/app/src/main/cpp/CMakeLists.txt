cmake_minimum_required(VERSION 3.22.1)

project("jaydesk_compositor")

# Add the native JNI bridge source
add_library(
    jaydesk_compositor
    SHARED
    compositor_jni.cpp
)

# Search for standard Android libraries
find_library(log-lib log)
find_library(android-lib android)

# Link the libraries
target_link_libraries(
    jaydesk_compositor
    ${log-lib}
    ${android-lib}
)
