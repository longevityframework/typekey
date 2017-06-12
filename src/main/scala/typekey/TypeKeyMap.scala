package typekey

import scala.language.higherKinds

/** a map where the keys are [[TypeKey type keys]] with an upper bound, and the
 * values have a type parameter with the same bound. The key and value of each
 * key/value pair are constrained to match on that type parameter. For example,
 * suppose we are maintaining an inventory of computer parts:
 *
 * {{{
 * sealed trait ComputerPart
 * case class Memory(gb: Int) extends ComputerPart
 * case class CPU(mhz: Double) extends ComputerPart
 * case class Display(resolution: Int) extends ComputerPart
 * }}}
 * 
 * we can use a `TypeKeyMap` to store a list of parts for each kind of part:
 *
 * {{{
 * var partLists = TypeKeyMap[ComputerPart, List]()
 * partLists += Memory(2) :: Memory(4) :: Memory(8) :: Nil
 * partLists += CPU(2.2) :: CPU(2.4) :: CPU(2.6) :: Nil
 * partLists += Display(720) :: Display(1080) :: Nil
 * }}}
 *
 * now we can look up part lists by part type, with everything coming back as
 * the expected type:
 * 
 * {{{
 * val memories: List[Memory] = partLists[Memory]
 * memories.size should be (3)
 * val cpus: List[CPU] = partLists[CPU]
 * cpus.size should be (3)
 * val displays: List[Display] = partLists[Display]
 * displays.size should be (2)
 *
 * val cpu: CPU = partLists[CPU].head
 * cpu should equal (CPU(2.2))
 * val display: Display = partLists[Display].tail.head
 * display should equal (Display(1080))
 * }}}
 *
 * note that the API does not provide methods to add multiple key/value pairs at
 * a time, as each pair needs to be type-checked separately.
 *
 * (code presented here is in `typekey.typeKeyMap.ScaladocSpec`.)
 * 
 * @tparam TypeBound the upper bound on the type parameters passed to the
 * TypeKey and Val types
 * @tparam Val the parameterized type of the values in the map
 *
 * @see `src/test/scala/typekey/typeKeyMap/` for many more examples
 */
class TypeKeyMap[TypeBound, Val[_ <: TypeBound]] private (underlying: Map[Any, Any])
extends BaseTypeBoundMap[TypeBound, TypeKey, Val](underlying) {

  /** retrieves the value which is associated with the given type key.
   * 
   * throws java.util.NoSuchElementException when no value is mapped to the
   * supplied type param.
   * 
   * @tparam TypeParam the type param binding both the type key and the value
   */
  def apply[TypeParam <: TypeBound : TypeKey]: Val[TypeParam] = get[TypeParam].get

  /** optionally returns the value associated with the given type key
   * 
   * @tparam TypeParam the type param bounding both the type key and the value
   * @return an option value containing the value associated with type key in
   * this map, or None if none exists.
   */
  def get[TypeParam <: TypeBound : TypeKey]: Option[Val[TypeParam]] =
    underlying.get(typeKey[TypeParam]).asInstanceOf[Option[Val[TypeParam]]]

  /** returns the value associated with a type key, or a default value if the
   * type key is not contained in the map.
   *
   * @param default a computation that yields a default value in case no binding
   * for the type key is found in the map
   * @tparam TypeParam the type param bounding both the type key and the value
   * @return the value associated with type key if it exists, otherwise the
   * result of the `default` computation.
   */
  def getOrElse[TypeParam <: TypeBound : TypeKey](default: => Val[TypeParam]): Val[TypeParam] =
    underlying.getOrElse(typeKey[TypeParam], default).asInstanceOf[Val[TypeParam]]

  /** adds a typekey/value pair to this map, returning a new map.
   * 
   * @tparam TypeParam the type param bounding both the type key and the value
   * @tparam ValTypeParam the type param for the value type. this can be any type, provided that
   * `Val[ValTypeParam] <: Val[TypeParam])`
   * @param pair the typekey/value pair
   * @param valConforms a constraint ensuring that `Val[ValTypeParam] <: Val[TypeParam])`
   */
  def +[
    TypeParam <: TypeBound,
    ValTypeParam <: TypeBound](
    pair: (TypeKey[TypeParam], Val[ValTypeParam]))(
    implicit valConforms: Val[ValTypeParam] <:< Val[TypeParam])
  : TypeKeyMap[TypeBound, Val] =
    new TypeKeyMap[TypeBound, Val](underlying + pair)

  /** adds a typekey/value pair to this map, returning a new map. the type key
   * is inferred from the type of the supplied value.
   *
   * PLEASE NOTE: using this method when your `Val` type is contravariant in its
   * type parameter will not do what you might expect! when the compiler infers
   * type parameter `[TypeParam <: TypeBound]` from an argument of type
   * `Contra[TypeParam]`, where type `Contra` is defined as, e.g.,
   * `trait Contra[+T]`, it's always going to infer `TypeBound` as the
   * `TypeParam`. there seems to be nothing I can do within `TypeKeyMap` to
   * circumvent this. the easiest way to work around this problem is to specify
   * the type key yourself with
   * [[TypeKeyMap.+[TypeParam<:TypeBound,ValTypeParam<:TypeBound]* the alternate
   * method +]].
   *
   * @param value the value to add to the map
   * @param key the type key, which is inferred from the type of value
   * @tparam TypeParam the type param
   */
  def +[
    TypeParam <: TypeBound](
    value: Val[TypeParam])(
    implicit key: TypeKey[TypeParam])
  : TypeKeyMap[TypeBound, Val] =
    new TypeKeyMap[TypeBound, Val](underlying + (key -> value))

  /** takes the union of two type key maps with the same type params
   *
   * @param that the type key map to union with this type key map
   * @return a new type key map with the bindings of this map and that map
   */
  def ++(that: TypeKeyMap[TypeBound, Val]): TypeKeyMap[TypeBound, Val] =
    new TypeKeyMap[TypeBound, Val](this.underlying ++ that.underlying)

  /** tests whether this TypeKeyMap contains a binding for a type param
   *
   * @tparam TypeParam the type param binding both the type key and the value
   * @return `true` if there is a binding for type param in this map, `false` otherwise.
   */
  def contains[TypeParam <: TypeBound : TypeKey] = super.contains(typeKey[TypeParam])

  /** selects all elements of this TypeKeyMap which satisfy a predicate
   *
   * @param p the predicate used to test elements
   * @return a new TypeKeyMap consisting of all elements of this TypeKeyMap that
   * satisfy the given predicate `p`. the order of the elements is preserved.
   */
  def filter(p: TypeBoundPair[TypeBound, TypeKey, Val, _ <: TypeBound] => Boolean)
  : TypeKeyMap[TypeBound, Val] = {
    val underlyingP: ((Any, Any)) => Boolean = { pair =>
      type TypeParam = TP forSome { type TP <: TypeBound }
      val tbp = TypeBoundPair[TypeBound, TypeKey, Val, TypeParam](
        pair._1.asInstanceOf[TypeKey[TypeParam]],
        pair._2.asInstanceOf[Val[TypeParam]])
      p(tbp)
    }
    new TypeKeyMap[TypeBound, Val](underlying.filter(underlyingP))
  }

  /** filters this map by retaining only keys satisfying a predicate
   *
   * @param p the predicate used to test keys
   * @return an immutable map consisting only of those key value pairs of this
   * map where the key satisfies the predicate `p`
   */
  def filterKeys(p: (TypeKey[_ <: TypeBound]) => Boolean): TypeKeyMap[TypeBound, Val] =
    new TypeKeyMap[TypeBound, Val](
      underlying.filterKeys { any => p(any.asInstanceOf[TypeKey[_ <: TypeBound]]) })

  /** selects all elements of this TypeKeyMap which do not satisfy a predicate
   *
   * @param p the predicate used to test elements
   * @return a new TypeKeyMap consisting of all elements of this TypeKeyMap that
   * do not satisfy the given predicate `p`. the order of the elements is
   * preserved.
   */
  def filterNot(p: TypeBoundPair[TypeBound, TypeKey, Val, _ <: TypeBound] => Boolean)
  : TypeKeyMap[TypeBound, Val] =
    filter((pair) => !p(pair))

  /** filters this map by retaining only pairs that meet the stricter type
   * bound `TypeBound2`. returns a `TypeKeyMap` with the tighter type bound
   *
   * @tparam TypeBound2 the new type bound
   */
  def filterTypeBound[TypeBound2 <: TypeBound : TypeKey]: TypeKeyMap[TypeBound2, Val] =
    new TypeKeyMap[TypeBound2, Val](
      underlying.filterKeys(_.asInstanceOf[TypeKey[_]] <:< typeKey[TypeBound2]))

  /** filters this map by retaining only values satisfying a predicate
   * 
   * @param p the predicate used to test values
   * @return an immutable map consisting only of those key value pairs of this
   * map where the value satisfies the predicate `p`
   */
  def filterValues(p: (Val[_ <: TypeBound]) => Boolean): TypeKeyMap[TypeBound, Val] =
    new TypeKeyMap[TypeBound, Val](
      underlying.filter { case (k, v) => p(v.asInstanceOf[Val[_ <: TypeBound]]) })

  /** transforms this type key map by applying a function to every retrieved value.
   *
   * @tparam NewVal the new value type for the resulting map
   * @param f the function used to transform values of this map
   * @return a map which maps every key of this map to f(this(key))
   */
  def mapValues[NewVal[_ <: TypeBound]](f: TypeBoundFunction[TypeBound, Val, NewVal])
  : TypeKeyMap[TypeBound, NewVal] = 
    new TypeKeyMap[TypeBound, NewVal](mapValuesUnderlying[TypeBound, NewVal](f))

  /** returns the same `TypeKeyMap`, but with the value type as a wider type
   * than the original value type
   *
   * @tparam Val2 the new value type
   */
  def widen[Val2[TypeParam <: TypeBound] >: Val[TypeParam]]: TypeKeyMap[TypeBound, Val2] =
    this.asInstanceOf[TypeKeyMap[TypeBound, Val2]]

  /** transforms this type key map into a type key map with a wider type bound
   * by applying a function to every retrieved value
   *
   * @tparam WiderTypeBound the new type bound for the resulting map
   * @tparam NewVal the new value type for the resulting map
   * @param f the function used to transform values of this map.
   * @return a map which maps every key of this map to `f(this(key))`.
   */
  def mapValuesWiden[
    WiderTypeBound >: TypeBound,
    NewVal[_ <: WiderTypeBound]](
    f: WideningTypeBoundFunction[TypeBound, WiderTypeBound, Val, NewVal])
  : TypeKeyMap[WiderTypeBound, NewVal] =
    new TypeKeyMap[WiderTypeBound, NewVal](
      mapValuesUnderlying[WiderTypeBound, NewVal](f))

  override def hashCode = underlying.hashCode

  /** compares two maps structurally; i.e., checks if all mappings contained in
   * this map are also contained in the other map, and vice versa
   * 
   * @param that the other type key map
   * @return true iff both are type key maps and contain exactly the same mappings
   */
  override def equals(that: Any) = {
    that.isInstanceOf[TypeKeyMap[TypeBound, Val]] &&
    that.asInstanceOf[TypeKeyMap[TypeBound, Val]].underlying == underlying
  }

  /** a string representation of a TypeKeyMap */
  override def toString = s"TypeKey${underlying}"

}

object TypeKeyMap {

  /** creates and returns an empty [[TypeKeyMap]] for the supplied types
   * 
   * @tparam TypeBound the upper bound on the type parameters passed to the
   * `TypeKey` and `Val` types
   * @tparam Val the parameterized type of the values in the map
   */
  def apply[TypeBound, Val[_ <: TypeBound]](): TypeKeyMap[TypeBound, Val] =
    new TypeKeyMap[TypeBound, Val](Map.empty)

}
