
cmake_minimum_required(VERSION 3.10)
project(AutelLibWrapper)

set(CMAKE_CXX_STANDARD 11)

# Android configuration
if(ANDROID)
    set(CMAKE_ANDROID_STL_TYPE c++_shared)
endif()

# Find all wrapper source files
file(GLOB WRAPPER_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/*.cpp")

# Create wrapper libraries
foreach(wrapper_src ${WRAPPER_SOURCES})
    get_filename_component(wrapper_name ${wrapper_src} NAME_WE)
    add_library(${wrapper_name} SHARED ${wrapper_src})
    
    if(ANDROID)
        target_link_libraries(${wrapper_name} 
            android
            log
            dl
        )
    else()
        target_link_libraries(${wrapper_name} 
            dl
            pthread
        )
    endif()
endforeach()
