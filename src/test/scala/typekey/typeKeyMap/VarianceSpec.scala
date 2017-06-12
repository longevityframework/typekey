package typekey.typeKeyMap

import typekey.TypeKeyMap
import typekey.typeKey
import org.scalatest.FlatSpec
import org.scalatest.GivenWhenThen
import org.scalatest.Matchers

/** tests [[TypeKeyMap]] operations in the face of covariant and contravariant types */
class VarianceSpec extends FlatSpec with GivenWhenThen with Matchers {

  behavior of "TypeKeyMaps in the face of someone trying to break through my bulletpoof type signatures"

  it should "thwart the attack with a compile time error of unspecified opacity" in {
    import typekey.testData.pets._
    val dog = Dog("dog")
    val hound = Hound("hound")

    // standard usage is to map dogs to dogs and hounds to hounds
    var petMap = TypeKeyMap[Pet, PetIdentity]()
    petMap += dog // type key is inferred
    petMap[Dog] should equal (dog)

    petMap = TypeKeyMap[Pet, PetIdentity]()
    petMap += typeKey[Dog] -> dog // type key is specified
    petMap[Dog] should equal (dog)

    // you are not allowed to map a hound to a dog. otherwise you
    // could then call "petToPetMap(hound)" and get a
    // ClassCastException.
    "petMap += typeKey[Hound] -> dog" shouldNot compile

    // you are allowed to map a dog to a hound. in this case
    // petMap[Dog] returns something of type PetIdentity[Dog] (which
    // =:= Dog), and the something returned is hound. nothing weird,
    // no ClassCastException.
    "petMap += typeKey[Dog] -> hound" should compile

    petMap = TypeKeyMap[Pet, PetIdentity]()
    petMap += typeKey[Dog] -> hound
    petMap[Dog] should equal (hound)

    // standard usage is to map dogs to dogs and hounds to hounds
    val dogInvar = new PetBoxInvar(dog)
    val houndInvar = new PetBoxInvar(hound)
    var petBoxInvarMap = TypeKeyMap[Pet, PetBoxInvar]()
    petBoxInvarMap += dogInvar
    petBoxInvarMap[Dog] should be theSameInstanceAs dogInvar

    petBoxInvarMap = TypeKeyMap[Pet, PetBoxInvar]()
    petBoxInvarMap += typeKey[Dog] -> dogInvar
    petBoxInvarMap[Dog] should be theSameInstanceAs dogInvar

    // you are not allowed to map a hound to a PetBoxInvar[Dog].
    // otherwise you could then call "petBoxInvarMap[Hound]" and get a
    // ClassCastException.
    "petBoxInvarMap += typeKey[Hound] -> dogInvar" shouldNot compile

    // you are not allowed to map a dog to a PetBoxInvar[Hound].
    // otherwise you could then call "petToPetBoxInvarMap(dog)" and
    // get a ClassCastException.
    "petBoxInvarMap += typeKey[Dog] -> houndInvar" shouldNot compile

    // standard usage is to map dogs to dogs and hounds to hounds
    val dogCovar = new PetBoxCovar(dog)
    val houndCovar = new PetBoxCovar(hound)
    var petBoxCovarMap = TypeKeyMap[Pet, PetBoxCovar]()
    petBoxCovarMap += dogCovar
    petBoxCovarMap[Dog] should be theSameInstanceAs dogCovar

    petBoxCovarMap = TypeKeyMap[Pet, PetBoxCovar]()
    petBoxCovarMap += typeKey[Dog] -> dogCovar
    petBoxCovarMap[Dog] should be theSameInstanceAs dogCovar

    // you are not allowed to map a hound to a PetBoxCovar[Dog].
    // otherwise you could then call "petBoxCovarMap[Hound]" and get a
    // ClassCastException.
    "petBoxCovarMap += typeKey[Hound] -> dogCovar" shouldNot compile

    // you are allowed to map a dog to a PetBoxCovar[Hound].  in this
    // case petToPetMap(dog) returns something of type
    // PetBoxCovar[Dog], which is >:> PetBoxCovar[Hound]
    petBoxCovarMap = TypeKeyMap[Pet, PetBoxCovar]()
    petBoxCovarMap += typeKey[Dog] -> houndCovar
    petBoxCovarMap[Dog] should be theSameInstanceAs houndCovar

    // standard usage is to map dogs to dogs and hounds to hounds
    val dogContravar = new PetBoxContravar
    val houndContravar = new PetBoxContravar
    var petBoxContravarMap = TypeKeyMap[Pet, PetBoxContravar]()

    // this next line of code DOES NOT do what you might expect!
    // when the compiler infers type parameter [TypeParam <:
    // TypeBound] from an argument of type Contra[TypeParam], where
    // type Contra is defined e.g. trait Contra[+T], it's always going
    // to pick TypeParam as TypeBound. there seems to be nothing i can
    // do within TypeKeyMap to circumvent this.
    petBoxContravarMap += dogContravar
    // the inferred TypeKey was not Dog
    intercept[NoSuchElementException] { petBoxContravarMap[Dog] }
    // the inferred TypeKey was Pet
    petBoxContravarMap[Pet] should be theSameInstanceAs dogContravar

    // with contravariance you will want to explicitly specify the
    // type key like so, and not try to infer it as above
    petBoxContravarMap = TypeKeyMap[Pet, PetBoxContravar]()
    petBoxContravarMap += typeKey[Dog] -> dogContravar
    petBoxContravarMap[Dog] should be theSameInstanceAs dogContravar

    // you are allowed to map a hound to a PetBoxContravar[Dog].  in
    // this case petBoxContravarMap[Hound] returns something of type
    // PetBoxContravar[Hound], which is >:> PetBoxContravar[Dog]
    petBoxContravarMap = TypeKeyMap[Pet, PetBoxContravar]()
    petBoxContravarMap += typeKey[Hound] -> dogContravar
    petBoxContravarMap[Hound] should be theSameInstanceAs dogContravar    

    // you are not allowed to map a dog to a PetBoxContravar[Hound].
    // otherwise you could then call "petToPetBoxContravarMap(dog)"
    // and get a ClassCastException.
    "petToPetBoxCovarMap += dog -> new PetBoxContravar[Hound]" shouldNot compile    
  }

}
