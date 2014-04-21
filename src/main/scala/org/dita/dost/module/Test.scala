package org.dita.dost.module

import java.io.File

class Test(ditaDir: File) extends Preprocess(ditaDir) {

  override val transtype = "test"

  override def run() {
    logger.info("test:")
    buildInit()
    preprocess()
  }

}
