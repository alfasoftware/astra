package org.alfasoftware.astra.core.refactoring.types.newpackage;

import org.alfasoftware.astra.core.refactoring.types.TypeReferenceExample;

@SuppressWarnings("rawtypes")
public class UpdatedTypeExampleAfter {

  Class clazzSimpleNameReference = UpdatedTypeExampleAfter.class;
  Class clazzQualifiedNameReference = org.alfasoftware.astra.core.refactoring.types.newpackage.UpdatedTypeExampleAfter.class;
  
  Class anotherClazzFromSamePackageReference = TypeReferenceExample.class;
  Class anotherClazzFromSamePackageQualifiedReference = org.alfasoftware.astra.core.refactoring.types.TypeReferenceExample.class;
}

