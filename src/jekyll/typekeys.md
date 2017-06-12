---
layout: default
---

## Type Keys

Because Scala generics are built on top of Java generics in the JVM, they suffer the same [problems with type-erasure](http://en.wikipedia.org/wiki/Generics_in_Java#Problems_with_type_erasure). We effectively lose track of the actual values of the type arguments for the generic type. Thankfully, Scala provides `TypeTags` to get around this problem. Type tags are explained in detail in other places, such as on the [Scala website](http://docs.scala-lang.org/overviews/reflection/typetags-manifests.html), and in this [Stack Overflow answer](http://stackoverflow.com/a/12232195/2186890), so I won't go into the details here.

As the Stack Overflow answer above points out, type tags are not necessarily equal, as in `==` equal, even though the two type tags represent an equivalent type.

To see this in action, let's boot up the REPL (Scala's interactive shell). I'm going to start it up inside SBT in the typekey project. This procedure should work for any project that declares a dependency on typekey:

```bash
bash% git clone https://github.com/longevityframework/typekey.git
bash% cd typekey
bash% sbt
> console
Welcome to Scala version 2.11.5 (Java HotSpot(TM) 64-Bit Server VM, Java 1.8.0_25).
scala> 
```
Okay, now we're in the REPL. Let's import all the type-taggy stuff from the Scala reflection library:

```scala
scala> import scala.reflect.runtime.universe._
import scala.reflect.runtime.universe._
```

Let's start with a simple type alias as an example. The type tags are not equal:

```scala
scala> trait A
defined trait A

scala> type B = A
defined type alias B

scala> typeTag[A] == typeTag[B]
res0: Boolean = false
```

The proper way to determine if these are equivalent types is with the `=:=` operator on `scala.reflect.api.Types.Type`, which can be accessed with method `TypeTag.tpe`:

```scala
scala> typeTag[A].tpe =:= typeTag[B].tpe
res1: Boolean = true
```

Or, equivalently:

```scala
scala> typeOf[A] =:= typeOf[B]
res2: Boolean = true
```

Note that `TypeTag.==` will fail us not only when the types are equivalent as above, but in some circumstances when they are exactly the same type:

```scala
scala> object x {
     |   trait C
     |   val ctag = typeTag[C]
     | }
defined object x

scala> x.ctag == typeTag[x.C]
res3: Boolean = false

scala> x.ctag.tpe =:= typeTag[x.C].tpe
res4: Boolean = true
```

Thankfully, we don't get a new, non-equal type tag every time the `typeTag` method is called:

```scala
scala> typeTag[A] == typeTag[A]
res5: Boolean = true
```

Clearly, type tags are not going to work very well as keys in a map. But `TypeKeys` are. Let's give it a try:

```scala
scala> import typekey.typeKey
import typekey.typeKey

scala> typeKey[A] == typeKey[B]
res6: Boolean = true

scala> object x {
     |   trait C
     |   val ckey = typeKey[C]
     | }
defined object x

scala> x.ckey == typeKey[x.C]
res7: Boolean = true
```

Because there is an implicit conversion from `TypeTag` to `TypeKey`, we can basically use a key anywhere we can use a tag. Common usage of type tags is like so:

```scala
scala> def grokTag[A : TypeTag] = println(typeTag[A].tpe)
grokTag: [A](implicit evidence$1: reflect.runtime.universe.TypeTag[A])Unit

scala> grokTag[List[_]]
scala.List[_]

scala> grokTag[List[Int]]
scala.List[Int]
```

We can do the exact same thing with type keys:

```scala
scala> import typekey.TypeKey
import typekey.TypeKey

scala> def grokKey[A : TypeKey] = println(typeKey[A].tpe)
grokKey: [A](implicit evidence$1: typekey.TypeKey[A])Unit

scala> grokKey[List[_]]
scala.List[_]

scala> grokKey[List[Int]]

scala.List[Int]
```

Either of the above grokking methods could have alternatively made use of Scala's implicitly:

```scala
scala> def grokTag[A : TypeTag] =
     |   println(implicitly[TypeTag[A]].tpe)
grokTag: [A](implicit evidence$1: reflect.runtime.universe.TypeTag[A])Unit

scala> def grokKey[A : TypeKey] = 
     |   println(implicitly[TypeKey[A]].tpe)
grokKey: [A](implicit evidence$1: typekey.TypeKey[A])Unit
```

You can also manually convert between tags and keys yourself:

```scala
scala> val tag = typeTag[A]
tag: reflect.runtime.universe.TypeTag[A] = TypeTag[A]

scala> TypeKey(tag)
res8: typekey.TypeKey[A] = TypeKey[A]

scala> val key = typeKey[A]
key: typekey.TypeKey[A] = TypeKey[A]

scala> key.tag
res9: reflect.runtime.universe.TypeTag[A] = TypeTag[A]
```

Of course, the `TypeKey.hashCode` method is consistent with `equals`, so we can use them as keys in sets and maps. To see this, let's set up some types to test with:

```scala
scala> trait A1
scala> type A2 = A1
scala> trait B1
scala> type B2 = B1
scala> trait C1
scala> type C2 = C1
```

And now, some sets:

```scala
scala> val tagset1 = Set(
     |   typeTag[A1], typeTag[A2],
     |   typeTag[B1], typeTag[B2],
     |   typeTag[C1], typeTag[C2])
scala> val tagset2 = Set(typeTag[A1], typeTag[B1], typeTag[C1])
scala> val keyset1 = Set(
     |   typeKey[A1], typeKey[A2],
     |   typeKey[B1], typeKey[B2],
     |   typeKey[C1], typeKey[C2])
scala> val keyset2 = Set(typeKey[A1], typeKey[B1], typeKey[C1])
```

The size of these sets are 6, 3, 3 and 3, respectively. This is how they equal:

```scala
scala> tagset1 == tagset2
res10: Boolean = false

scala> keyset1 == keyset2
res11: Boolean = true

scala> tagset1.map(TypeKey(_)) == keyset1
res12: Boolean = true
```

Clearly, a set of type tags does not have any conceptual correlation to a set of types. But the set of type keys does.

Above, when we called methods `grokTag` and `grokKey`, we specified the type argument explicitly, with calls like `grokTag[A]` and `grokKey[A]`. We can also lock down the type argument another way: by explicitly specifying the implicit arguments for `TypeTag` and `TypeKey`:

```scala
scala> grokTag(typeTag[List[Int]])
scala.List[Int]

scala> grokKey(typeKey[List[Int]])
scala.List[Int]
```

We can do the same thing with the tags and keys in our sets:

```scala
scala> tagset1.foreach(grokTag(_))
C2
A1
B2
A2
B1
C1

scala> tagset2.foreach(grokTag(_))
A1
B1
C1

scala> keyset1.foreach(grokKey(_))
A1
B1
C1

scala> keyset2.foreach(grokKey(_))
A1
B1
C1
```

We can also use type keys as keys in maps. For instance:

```scala
scala> var counter = Map[TypeKey[_], Int]()
counter: scala.collection.immutable.Map[typekey.TypeKey[_],Int] = Map()
```

Here's a function to increment the counter for a given key:

```scala
scala> def countKey[A : TypeKey]: Unit =
     |   counter += typeKey[A] ->
     |     (counter.getOrElse(typeKey[A], 0) + 1)
countKey: [A](implicit evidence$1: typekey.TypeKey[A])Unit
```

Let's see how it works:

```scala
scala> counter(typeKey[A1])
java.util.NoSuchElementException: key not found: TypeKey[A1]
  at scala.collection.MapLike$class.default(MapLike.scala:228)
  at scala.collection.AbstractMap.default(Map.scala:59)
  at scala.collection.MapLike$class.apply(MapLike.scala:141)
  at scala.collection.AbstractMap.apply(Map.scala:59)
  ... 43 elided

scala> counter(typeKey[A2])
java.util.NoSuchElementException: key not found: TypeKey[A2]

scala> countKey[A1]

scala> counter(typeKey[A1])
res13: Int = 1

scala> counter(typeKey[A2])
res14: Int = 1

scala> countKey[A1]

scala> countKey[A2]

scala> countKey[A1]

scala> countKey[A2]

scala> counter(typeKey[A1])
res15: Int = 5

scala> counter(typeKey[A2])
res16: Int = 5
```

By this point, we've exercised all the basic functionality of a `TypeKey`.  Do you have any ideas of real-life scenarios where these might come in handy? We'll start looking at a more realistic use-case in the [next chapter](typekeymaps.html), when we begin to investigate `TypeKeyMaps`.
