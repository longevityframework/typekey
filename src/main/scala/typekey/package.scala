import scala.reflect.api.Mirror
import scala.reflect.api.TypeCreator
import scala.reflect.api.Universe
import scala.reflect.runtime.universe.RuntimeMirror
import scala.reflect.runtime.universe.Type
import scala.reflect.runtime.universe.Type
import scala.reflect.runtime.universe.TypeTag

/** higher kinded type-polymorphic collections */
package object typekey {

  /** returns a [[TypeKey]] for the specified type `A`. this method will only work where a `TypeTag`
   * is implicitly available.
   */
  def typeKey[A : TypeKey]: TypeKey[A] = implicitly[TypeKey[A]]

  /** an implicit method for producing a [[TypeKey]]. this method allows type keys to be available
   * implicitly anywhere that the corresponding `TypeTag` is implicitly available.
   */
  implicit def typeKeyFromTag[A : TypeTag]: TypeKey[A] = TypeKey(implicitly[TypeTag[A]])

  private[typekey] def typeFullname(tpe: Type) = tpe.typeSymbol.fullName.toString

  private[typekey] def typeName(tpe: Type) = tpe.typeSymbol.name.decodedName.toString

  private[typekey] def typeNamePrefix(tpe: Type) = {
    val fullname = typeFullname(tpe)
    fullname.substring(0, fullname.lastIndexOf('.'))
  }

  // overloaded makeTypeTag follows FixedMirrorTypeCreator in
  // https://github.com/scala/scala/blob/2.11.x/src/reflect/scala/reflect/internal/StdCreators.scala

  private[typekey] def makeTypeTag[A](tpe: Type, mirror: RuntimeMirror): TypeTag[A] = {
    val typeCreator = new TypeCreator {
      def apply[U <: Universe with Singleton](m: Mirror[U]): U # Type =
        tpe.asInstanceOf[U # Type]
    }
    TypeTag[A](mirror, typeCreator)
  }

}
