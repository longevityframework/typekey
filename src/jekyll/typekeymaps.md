---
layout: default
---

## Type Key Maps

In the [previous chapter](typekeys.html), we saw how emblem's `TypeKeys` can be used in much the same way as `TypeTags`. Unlike `TypeTags`, `TypeKeys` compare as equal (as in `==`) for equivalent types. We saw how this allowed us to create sets of types, and use types as keys in a map. The map example we presented, however, was not very interesting, as the values stored were just `Ints`. What if we wanted to do something more interesting than that, where the types of the values actually depended on the types of the keys?

Let's take a look at an example to see what I'm getting at. Suppose we are developing an application for a company that builds computers according to the customers' specifications. We want to model the different kinds of computer parts, and how they fit together to form a computer. We might come up with something like this:

```scala
sealed trait ComputerPart
case class Memory(gb: Int) extends ComputerPart
case class CPU(mhz: Double) extends ComputerPart
case class Display(resolution: Int) extends ComputerPart

case class Computer(memory: Memory, cpu: CPU, display: Display)
```

In the workshop where we build the computers, we have an inventory of computer parts that customers can select. We'll store each choice for `Memory` in a `List[Memory]`, each available `CPU` in a `List[CPU]`, and so on:

```scala
val memoryList = Memory(2) :: Memory(4) :: Memory(8) :: Nil
val cpuList = CPU(2.2) :: CPU(2.4) :: CPU(2.6) :: Nil
val displayList = Display(720) :: Display(1080) :: Nil
```

We anticipate adding more kinds of `ComputerParts` in the future, and we are not terribly comfortable with having individual `Lists` around for each kind of part. We really want to store the entire inventory in a single data structure. We settle on building a `Map` where the keys to the map are the type of computer part, and the values are the list of parts of that type. The keys have this type:

```scala
TypeKey[_ <: ComputerPart]
```

And the values have this type:

```scala
List[_ <: ComputerPart]
```

We can construct our inventory using the hardcoded part lists we built above:

```scala
import emblem.TypeKey
import emblem.typeKey

val inventory =
  Map[TypeKey[_ <: ComputerPart], List[_ <: ComputerPart]](
    typeKey[Memory] -> memoryList,
    typeKey[CPU] -> cpuList,
    typeKey[Display] -> displayList)
```

We can pull up our inventory of `CPUs` like so:

```scala
val cpus = inventory(typeKey[CPU])
```

But there is a problem. The type of the resulting value is not right. It is `List[_ <: ComputerPart]`, when we are expecting `List[CPU]`. If we need to use it as a `List[CPU]`, we will have to cast it to the right type. Consider what a method would look like that pulls a parts list out of the inventory by type. We want it to take in the type as parameter, and return a properly typed list. In other words, we want a method signature that looks something like this:

```scala
def partList[P <: ComputerPart]: List[P]
```

We would call this method like so:

```scala
val cpus = partList[CPU]
```

Since we need to look up the part list by `TypeKey`, we can add a `TypeKey` as an implicit parameter, like so:

```scala
def partList[P <: ComputerPart : TypeKey]: List[P]
```

Within the body of this method, we have to cast from `List[_ <: ComputerPart]` to `List[P]`:

```scala
def partList[P <: ComputerPart : TypeKey]: List[P] =
  inventory(typeKey[P]).asInstanceOf[List[P]]
```

And of course, there is nothing preventing this typecast from failing, as inventory may have been mistakenly initialized like so:

```scala
val inventory =
  Map[TypeKey[_ <: ComputerPart], List[_ <: ComputerPart]](
    typeKey[Memory] -> memoryList,
    typeKey[CPU] -> displayList, // OOPS!
    typeKey[Display] -> displayList)
```

At this point, calling val `cpus = partList[CPU]` does fail, but not in the way you might think. Due to type erasure, the typecast is successful, and we end up with something with type `List[CPU]`, and value `List(Display(720), Display(1080))`! The  
`java.lang.ClassCastException` does not occur until we actually access one of the elements of the list, say with `cpus(0)` or `cpus.head`.

Let's sum up the problems we are having with this Map:

1. We are forced to typecast when accessing data in the map, and this makes us mildly nauseous.
2. Nothing is enforcing that the key and value in the map are working with the same kind of `ComputerPart`.

I created `TypeKeyMap` for situations like these. They behave similarly to the `Map` above, but the types of the key and the value in any key-value pair are forced to agree. To build a `TypeKeyMap`, we have to specify two type parameters: `TypeBound`, which serves as an upper bound on the type parameters for the key and value types; And `Val`, which describes the type of the values in the map. In our example, we want to use `ComputerPart` for `TypeBound`, and `List` for `Val`. We initialize our new inventory as follows:

```scala
import emblem.TypeKeyMap

val inventory = TypeKeyMap[ComputerPart, List]() +
  memoryList + cpuList + displayList
```
                                                       
There are a couple points of interest I'd like to mention here. First, notice that when we add a key-value pair to the map, we only need to mention the value, as the key can be inferred from the value. If you want to be explicit about the keys as well, the following code is equivalent:

```scala
val inventory = TypeKeyMap[ComputerPart, List]() +
  (typeKey[Memory] -> memoryList) +
  (typeKey[CPU] -> cpuList) +
  (typeKey[Display] -> displayList)
```

Second, in contrast to `scala.collection.immutable.Map`, there is no way to initialize the `TypeKeyMap` with a single varargs method invocation. This is because every key-value pair must be type-checked individually, to make sure the type bounds match. I have an idea about how I might overcome this problem, but it's not high priority for me at the moment, since using the `+` operator as above to construct the map seems elegant enough.

Now, when we pull out a part list, it is well typed. In this example, we look up a value explicitly by key, just as we would with a normal Scala `Map`. No cast required:

```scala
val memParts: List[Memory] = inventory(typeKey[Memory])
```

But that's a little verbose. Here's a more fluid way to look up a value by key:

```scala
val memParts: List[Memory] = inventory[Memory]
```

We can update the `TypeKeyMap` using the `+` operator. `TypeKeyMaps` are immutable, so this operation produces a new map:

```scala
val moreMemory = Memory(16) :: memoryList
val updatedInventory = inventory + moreMemory
```

Once again, the `TypeKey` is resolved implicitly, but we can be explicit about it if we like:

```scala
val updatedInventory = inventory + (typeKey[Memory] -> moreMemory)
```

Of course, providing a mistyped key-value pair doesn't work. If we say:

```scala
val updatedInventory = inventory + (typeKey[CPU] -> moreMemory)
```

We get the following compile-time error:

```scala
Cannot prove that List[CPU] <:< List[Memory].
```

Ideally, the `TypeKeyMap` API would be contain analogs for everything in `scala.collection.immutable.Map`. And while I would really like to see that happen, so far I have only implemented the easy parts of the API, and the methods that I wanted to use myself. Here's a rough list of the methods I have implemented so far:

- `+`, `++`, `apply`, `contains`, `equals`, `filter`, `filterKeys`, `filterNot`, `foreach`, `get`, `getOrElse`, `hashCode`, `isEmpty`, `iterator`, `keys`, `mapValues`, `size`, `toString`, `values`

I also put in a `filterValues`, which is not available on a standard `Map`, but saves the user some trouble, as the method signature is much easier to digest than that of `filter`.

I've found reproducing some portions of the `Map` API to be a little bit tricky, and to require the introduction of supporting classes. For instance, consider foreach. In the `Map` API, the method has the following signature:

```scala
def foreach(f: ((A, B)) ⇒ Unit): Unit
```

A little thought shows this signature to be insufficient for a `TypeKeyMap`. In our example above, the function `f` passed to method foreach would have a signature like this:

```scala
def f(pair: (TypeKey[_ <: ComputerPart],
             List[_ <: ComputerPart])): Unit
```

But what we actually want is a `TypeKey`/`ComputerPart` pair where the type parameters match. In other words, we want to supply a function with a signature like this:

```scala
def f[P <: ComputerPart](pair: (TypeKey[P], List[P]): Unit
```

We run into a stumbling block at this point, because anonymous functions in Scala cannot yet have type parameters. So to make foreach happen, I had to introduce a supporting class, `TypeBoundPair`.

I would really be thrilled if you were to start using this code, so if you find yourself wanting to use a part of the Map API that is not reflected here, please just let me know and I will do my best to add it in a timely manner. (Of course, I would be even more thrilled to see a pull request!)

For more examples of using `TypeKeyMaps`, take a look at the
[TypeKeyMap unit tests](https://github.com/longevityframework/typekey/blob/master/src/test/scala/typekey/typeKeyMap).
