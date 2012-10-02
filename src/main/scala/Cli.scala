import java.io.File
import org.dita.dost.module._

object Cli {

  def main(args: Array[String]): Unit = {
    val wordrtf = new Dita2wordrtf()
    Properties("args.input") = new File("/Users/jelovirt/Temp/extname/test.ditamap").getAbsolutePath()
    Properties("transtype") = "xhtml"
    wordrtf.dita2wordrtf()
  }

}