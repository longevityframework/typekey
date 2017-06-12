---
layout: default
---

## Using Types that do not have Exactly One Type Parameter

It's a bit of a limitation that our `TypeKeyMaps` and `TypeBoundMaps` only take key and value types with a single type parameter. What if we wanted to use these kinds of maps with simpler, or more complicated types? As an example, let's revisit our "UserComment" system from the [previous chapter](typeboundmaps.html). A quick summary of our `Entity` and `EntityType` hierarchies:

```scala
trait Entity
class User extends Entity
class Comment extends Entity

trait EntityType[E <: Entity]
object User extends EntityType[User]
object Comment extends EntityType[Comment]
```

Let's say we are targeting two kinds of databases: MongoDB and an in-memory database. We might introduce the following type hierarchy:

```scala
sealed trait PersistenceType
case object Mongo extends PersistenceType
case object InMem extends PersistenceType
```

Now we want to modify our repository type so that it also takes a `PersistenceType` as a type parameter:

```scala
trait NewRepository[E <: Entity, P <: PersistenceType]

class UserRepo extends NewRepository[User, Mongo.type]
class CommentRepo extends NewRepository[Comment, Mongo.type]

class TestUserRepo extends NewRepository[User, InMem.type]
class TestCommentRepo extends NewRepository[Comment, InMem.type]
```

We just broke our repository map, that used to look something like this:

```scala
import emblem.typeBound.TypeBoundMap

// does not compile!
val repositories =
  TypeBoundMap[Entity, EntityType, NewRepository]() +
  (User -> userRepo) +
  (Comment -> commentRepo)
```

This now gives a compiler error: `NewRepository takes two type parameters, expected: one`.

You might think that we would need to implement a _new_ kind of `TypeBoundMap`; one where the value type takes two parameters. And we would need _another_ type of `TypeBoundMap` where the second type parameter, and not the first, is the one that varies with the key's type parameter. And so on for all the possible combinations we would want to handle. Thankfully, Scala's type system is flexible enough so that we don't have to do this. Instead, we need to introduce a new type that effectively transforms the two-type parameter type into a one-type parameter type. In our case, something like this:

```scala
type RepoAnyPersistenceType[E <: Entity] = NewRepository[E, _ <: PersistenceType]
```

Now, a `RepoAnyPersistenceType[User]` is the same type as `NewRepository[User, _ <: PersistenceType]`, and `RepoAnyPersistenceType[Comment]` is the same as `NewRepository[Comment, _ <: PersistenceType]`. The new type works with a `TypeBoundMap`:

```scala
val repositories =
  TypeBoundMap[Entity, EntityType, RepoAnyPersistenceType]() +
  (User -> userRepo) +
  (Comment -> commentRepo)
```

Now, when we pull a repository out of the map, it types as follows:

```scala
val userRepo2: NewRepository[User, _ <: PersistenceType] = repositories(User)
```

When can use the same trick for a type with no type parameters. For instance, suppose we want to keep track of the repositories that manage given entities. Something like:

```scala
var managingRepoMap = Map[_ <: Entity, Repository[_ <: Entity]]()

// track that user1 is managed by userRepo1:
managingRepoMap += user1 -> userRepo1
```

Of course, using a Scala library map as above is going to lose type information, and require the use of a cast on the way out:

```scala
val managingRepo: Repository[User] = managingRepoMap(user1).asInstanceOf[Repository[User]]
```

But we can't just just a `TypeBoundMap` with the types we have so far. This doesn't compile:

```scala
var managingRepoMap = TypeBoundMap[Entity, Entity, Repository]() // does not compile!
```

The error message is not surprising: `Entity takes no type parameters, expected: one`

We can get around this again by introducing a new type:

```scala
type EntityIdentity[E <: Entity] = E
```

Now, `EntityIdentity[User]` is the same type as `User`, and `EntityIdentity[Comment]` is the same as `Comment`. And we can use it in our `TypeBoundMap`:

```scala
var managingRepoMap = TypeBoundMap[Entity, EntityIdentity, Repository]()
managingRepoMap += user1 -> userRepo
managingRepoMap += user2 -> testUserRepo
managingRepoMap += comment1 -> commentRepo
managingRepoMap += comment2 -> testCommentRepo
```

The same technique works with `TypeKeyMaps`, so we can build maps such as the following:

```scala
import emblem.TypeKeyMap

val repositories = TypeKeyMap[Entity, RepoAnyPersistenceType]()

// one representative entity for each every entity type:
val representatives = TypeKeyMap[Entity, EntityIdentity]()
```
