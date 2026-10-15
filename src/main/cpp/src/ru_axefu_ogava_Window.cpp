#include "ru_axefu_ogava_Window.h"
#include <jni.h>

#define GLEW_STATIC
#include <vector>
#include <GL/glew.h>
#include <GLFW/glfw3.h>

static bool glfwInitialized = false;
static std::vector<GLFWwindow*>* windows;

long init() {
  if (!glfwInitialized) {
    glfwInitialized = true;
#ifdef __linux__
    glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_X11);
#endif
    if (glfwInit() != GLFW_TRUE) {
      glfwInitialized = false;
      return -1;
    }
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
  }
  return 0;
}

JNIEXPORT jlong JNICALL Java_ru_axefu_ogava_Window_create(JNIEnv *env, jobject, jint width, jint height, jstring title) {
  int initResult = init();
  if (initResult != 0) {
    return initResult;
  }

  const char* cstr = env->GetStringUTFChars(title, nullptr);
  GLFWwindow* window = glfwCreateWindow(width, height, cstr, nullptr, nullptr);
  env->ReleaseStringUTFChars(title, cstr);

  if (window == nullptr) {
    glfwTerminate();
    return -2;
  }

  if (windows == nullptr) {
    windows = new std::vector<GLFWwindow*>();
  }
  windows->push_back(window);

  glfwMakeContextCurrent(window);

  glewExperimental = true;
  if (glewInit() != GLEW_OK) {
    glfwTerminate();
    return -3;
  }

  int vw = 0;
  int vh = 0;
  glfwGetWindowSize(window, &vw, &vh);
  glViewport(0, 0, vw, vh);
  glfwMakeContextCurrent(nullptr);
  return (uintptr_t)window;
}

JNIEXPORT void JNICALL Java_ru_axefu_ogava_Window_close(JNIEnv *, jobject, jlong pointer) {
  GLFWwindow* window = (GLFWwindow*)(uintptr_t)pointer;
  glfwSetWindowShouldClose(window, GL_TRUE);
}

JNIEXPORT void JNICALL Java_ru_axefu_ogava_Window_loop(JNIEnv *, jclass) {
  while (windows->size() > 0) {
    glfwPollEvents();
    for (GLFWwindow* window : *windows) {
      if (!glfwWindowShouldClose(window)) {
        glfwMakeContextCurrent(window);
        glfwSwapBuffers(window);
        glfwMakeContextCurrent(nullptr);
      } else if (auto it = std::ranges::find(*windows, window); it != windows->end()) {
          glfwDestroyWindow(window);
          windows->erase(it);
        }
    }
  }
  delete windows;
  glfwTerminate();
}