---
layout: default
---

Welcome to the [typekey](https://github.com/longevityframework/typekey)! We privide an extension API
for [type tags](http://docs.scala-lang.org/overviews/reflection/typetags-manifests.html), called a
[type key](api/typekey/TypeKey.html), as well as some higher kinded type-polymorphic collections.

In brief, typekey provides the following features:

- A `TypeKey` is a
  [`TypeTag`](http://docs.scala-lang.org/overviews/reflection/typetags-manifests.html) that is
  suitable for use as a key in a `Set` or a `Map`.
- A `TypeKeyMap` is a `Map` from `TypeKey` to objects with type arguments that match the type
  indicated by the `TypeKey`.
- A `TypeBoundMap` is a `Map` where the key and value of each key-value pair is bound to the same
  type argument.

Here's some more in-depth material to help you get started:

1. [Experimenting in the Scala REPL](experiment.html)
1. [Setting up a Library Dependency on typekey](libdep.html)
1. [TypeKeys](typekeys.html)
1. [TypeKeyMaps](typekeymaps.html)
1. [TypeBoundMaps](typeboundmaps.html)
1. [Using types that do not have exactly one type parameter with TypeKeyMaps and TypeBoundMaps](tparams.html)
