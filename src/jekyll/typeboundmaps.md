---
layout: default
---

## Type Bound Maps

A `TypeBoundMap` is a map where the key and value types both take a type parameter. For any key-value pair in the map, the key and value will have the same type argument for their type parameter. Let's take a look at an example and see how this works.

Suppose we are building a system called "UserComment", where users can post comments about whatever is on their mind. We start out with two entity classes to represent our domain model: `User` and `Comment`. We choose to label our entity classes with an `Entity` trait, and we also provide a type class for every entity type, in which we store information such as the natural keys for that type:

```scala
trait Entity

trait EntityType[E <: Entity] {
  val naturalKeys: Seq[NaturalKey] = Seq()
}
```

We provide a default empty set of natural keys, but implementing classes want to override it where appropriate. Our convention is to have the companion object to the entity class implement the `EntityType`, like so:

```scala
case class User(userId: String, firstName: String, lastName: String)
extends Entity

object User extends EntityType[User] {
  override val naturalKeys = Seq(NaturalKey("User.userId"))
}
```

We also have repositories for each of our entity classes, like so:

```scala
trait Repository[E <: Entity]

class UserRepo extends Repository[User]
class CommentRepo extends Repository[Comment]
```

Now let's say we want to maintain a map from the entity type to the repository. We could try something like this:

```scala
val empty: Map[EntityType[_ <: Entity],
               Repository[_ <: Entity]] = Map()

val repositories = empty + (User -> userRepo) + (Comment -> commentRepo)
```

But the typing of this collection does not reflect the fact that for any given pair in the map, the key and value both have same kind of entity as its type argument. This means we have to cast when retrieving a repo. For instance:

```scala
val repositories: Map[EntityType[_ <: Entity],
                      Repository[_ <: Entity]] = initialize()

val userRepo: Repository[User] =
  repositories(User).asInstanceOf[Repository[User]]
```

There is no guarantee that the type cast will succeed. (Even worse, the type cast probably will succeed due to type erasure, and you'll get some other `ClassCastException` down the line.)

To solve this problem, we use a `TypeBoundMap` instead of a `Map`. The `TypeBoundMap` has three type parameters: the key type, the value type, and a bounds type that provides an upper bound for the type parameters to the key and value types. In our example, the key type is `EntityType`, the value type is `Repository` and the bounds type is `Entity`. We can build our `TypeBoundMap` like so:

```scala
import emblem.typeBound.TypeBoundMap

val repositories =
  TypeBoundMap[Entity, EntityType, Repository]() +
  (User -> userRepo) +
  (Comment -> commentRepo)
```

It's a compiler error to try to add a key-value pair to a `TypeBoundMap` when the type parameter for the key and value don't match, as in both of the following expressions:

```scala
repositories + (User -> commentRepo) // does not compile!
repositories + (Comment -> userRepo) // does not compile!
```

Pulling a repository out of the map is type-safe, and does not require a cast:

```scala
val userRepo: Repository[User] = repositories(User)
```

The [`TypeKeyMap`](TypeKeyMap) that we looked at in the last chapter is really just a special case of a `TypeBoundMap` where the key type is fixed as a `TypeKey`. It's included as a separate class so that we can make use of implicit `TypeKey` values for a more succinct API. For instance, consider the `ComputerPart` example from the previous chapter. We could have chosen to use a `TypeBoundMap[ComputerPart, TypeKey, List]` instead of a `TypeKeyMap[ComputerPart, List]`:

```scala
import emblem.TypeKeyMap
import emblem.typeKey

val inventories: TypeKeyMap[ComputerPart, List] = initializeInventories()
val inventories2: TypeBoundMap[ComputerPart, TypeKey, List] = initializeInventories2()

// longhand for adding a key-value pair to the map:
inventories + typeKey[Memory] -> memoryList
inventories2 + typeKey[Memory] -> memoryList

// shorthand only available for TypeKeyMap:
inventories + memoryList

// longhand for checking for containment:
inventories.contains(typeKey[Memory])
inventories2.contains(typeKey[Memory])

// shorthand only available for TypeKeyMap:
inventories.contains[Memory]

// longhand for retrieving a value:
val memList = inventories(typeKey[Memory])
val memList2 = inventories2(typeKey[Memory])

// shorthand only available for TypeKeyMap:
val memList3 = inventories[Memory]
```

Because `TypeKeyMap` and `TypeBoundMap` share a lot of underlying code, they both implement a similar subset of the `scala.collections.immutable.Map` API:

- `+`, `++`, `apply`, `contains`, `equals`, `filter`, `filterKeys`, `filterNot`, `filterValues`, `foreach`, `get`, `getOrElse`, `hashCode`, `isEmpty`, `iterator`, `keys`, `mapValues`, `size`, `toString`, `values`

I expect to continue to grow the API to mirror the Scala library map API as much as possible. If you are using `TypeBoundMaps`, and there is a method missing from the API that you would like to have, please let me know, and I will do my best to put it in.


For more examples of using `TypeBoundMaps`, take a look at the
[TypeBoundMap unit tests](https://github.com/longevityframework/typekey/blob/master/src/test/scala/typekey/typeBoundMap).
