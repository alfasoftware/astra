package org.alfasoftware.astra.example.target;

public class FooBarCaller {

  private final FooBarInterface fooBarInterface;

  FooBarCaller(FooBarInterface fooBarInterface){
    this.fooBarInterface = fooBarInterface;
  }

  void doThing(){
    fooBarInterface.doFoo();
  }
}