package bot.core

case class CoreException(description: String,
    cause: Throwable = null) extends RuntimeException(description, cause)