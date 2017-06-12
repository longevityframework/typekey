[![License](http://img.shields.io/:license-Apache%202-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

## Higher Kinded Type-Polymorphic Collections

In brief, typekey provides the following features:

- A `TypeKey` is a
  [`TypeTag`](http://docs.scala-lang.org/overviews/reflection/typetags-manifests.html) that is
  suitable for use as a key in a `Set` or a `Map`.
- A `TypeKeyMap` is a `Map` from `TypeKey` to objects with type arguments that match the type
  indicated by the `TypeKey`.
- A `TypeBoundMap` is a `Map` where the key and value of each key-value pair is bound to the same
  type argument.

For more details, please see the [typekey website](https://longevityframework.github.io/typekey/)
and [API docs](https://longevityframework.github.io/typekey/api/typekey).
