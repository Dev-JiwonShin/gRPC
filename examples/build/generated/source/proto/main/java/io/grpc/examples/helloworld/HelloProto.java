// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: helloworld.proto

package io.grpc.examples.helloworld;

public final class HelloProto {
  private HelloProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_helloworld_MyNumber_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_helloworld_MyNumber_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\020helloworld.proto\022\nhelloworld\"\031\n\010MyNumb" +
      "er\022\r\n\005value\030\n \001(\0052G\n\tMyService\022:\n\nMyFunc" +
      "tion\022\024.helloworld.MyNumber\032\024.helloworld." +
      "MyNumber\"\000B1\n\033io.grpc.examples.helloworl" +
      "dB\nHelloProtoP\001\242\002\003HLWb\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_helloworld_MyNumber_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_helloworld_MyNumber_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_helloworld_MyNumber_descriptor,
        new java.lang.String[] { "Value", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
