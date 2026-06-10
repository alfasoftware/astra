package org.alfasoftware.astra.core.refactoring.operations.sonar.s2959;

import java.io.InputStream;

public class TryWithResourcesTrailingSemicolon {

  void singleResourceWithTrailingSemicolon(InputStream input) throws Exception {
    try (InputStream i = input;) {
      i.read();
    }
  }

  void multipleResourcesWithTrailingSemicolon(InputStream i1, InputStream i2) throws Exception {
    try (InputStream a = i1; InputStream b = i2;) {
      a.read();
    }
  }

  void singleResourceNoTrailingSemicolon(InputStream input) throws Exception {
    try (InputStream i = input) {
      i.read();
    }
  }

  void multipleResourcesNoTrailingSemicolon(InputStream i1, InputStream i2) throws Exception {
    try (InputStream a = i1; InputStream b = i2) {
      a.read();
    }
  }
}
