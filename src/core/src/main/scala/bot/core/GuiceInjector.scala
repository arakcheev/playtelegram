package bot.core

import com.google.inject.Inject

import scala.reflect.ClassTag

class GuiceInjector @Inject() (injector: com.google.inject.Injector){

  /**
    * Get an instance of the given class from the injector.
    */
  def instanceOf[T](implicit ct: ClassTag[T]): T = instanceOf(ct.runtimeClass.asInstanceOf[Class[T]])

  /**
    * Get an instance of the given class from the injector.
    */
  def instanceOf[T](clazz: Class[T]): T = injector.getInstance(clazz)
}