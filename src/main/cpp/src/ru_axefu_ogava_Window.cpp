#include "ru_axefu_ogava_Window.h"

#define GLEW_STATIC
#include <GL/glew.h>
#include <GLFW/glfw3.h>
#include <jni.h>
#include <mutex>
#include <vector>

static bool glfwInitialized = false;
static std::mutex mtx;
static std::vector<GLFWwindow*>* windows;

long init() {
  if (!glfwInitialized) {
    glfwInitialized = true;
#ifdef __linux__
    glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_X11);
#endif
    if (glfwInit() != GLFW_TRUE) {
      return -1;
    }
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
  }
  return GLFW_TRUE;
}

JNIEXPORT jlong JNICALL Java_ru_axefu_ogava_Window_create(JNIEnv *env, jobject, jint width, jint height, jstring title) {
  std::lock_guard lock(mtx);
  if (init() != GLFW_TRUE) {
    return -1;
  }

  const char* cstr = env->GetStringUTFChars(title, nullptr);
  GLFWwindow* window = glfwCreateWindow(width, height, cstr, nullptr, nullptr);
  env->ReleaseStringUTFChars(title, cstr);

  if (window == nullptr) {
    glfwTerminate();
    return -2;
  }

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

  if (windows == nullptr) {
    windows = new std::vector<GLFWwindow*>();
  }
  windows->push_back(window);
  return (uintptr_t)window;
}

JNIEXPORT void JNICALL Java_ru_axefu_ogava_Window_close(JNIEnv *, jobject, jlong pointer) {
  GLFWwindow* window = (GLFWwindow*)(uintptr_t)pointer;
  glfwSetWindowShouldClose(window, GL_TRUE);
}

void tick() {
  std::lock_guard lock(mtx);
  glfwPollEvents();
  auto it = windows->begin();
  while (it != windows->end()) {
    GLFWwindow* window = *it;
    if (glfwWindowShouldClose(window)) {
      glfwDestroyWindow(window);
      it = windows->erase(it);
      continue;
    }

    glfwMakeContextCurrent(window);
    glfwSwapBuffers(window);
    glfwMakeContextCurrent(nullptr);

    ++it;
  }
}

JNIEXPORT void JNICALL Java_ru_axefu_ogava_Window_loop(JNIEnv *env, jclass clazz) {
  while (windows->size() > 0) {
    tick();
  }
  std::lock_guard lock(mtx);
  delete windows;
  windows = nullptr;
  glfwTerminate();
  glfwInitialized = false;
  jfieldID field = env->GetStaticFieldID(clazz, "glfwThread", "Ljava/lang/Thread;");
  env->SetStaticObjectField(clazz, field, nullptr);
}