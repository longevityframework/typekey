package typekey.testData

/** for type map happy cases */
object blogs {

  implicit def stringToEmail(email: String): Email = Email(email)

  implicit def stringToMarkdown(markdown: String): Markdown = Markdown(markdown)

  implicit def stringToUri(uri: String): Uri = Uri(uri)

  implicit def intToZipcode(zip: Int): Zipcode = Zipcode(zip)

  object CrmUser {

    private val dummyAddress = CrmAddress("street1", "street2", "city", "state", /*0*/1210)
    private val dummyUser = CrmUser(Uri("uri"), "firstName", "lastName", dummyAddress)

    def apply(uri: Uri): CrmUser = dummyUser.copy(uri = uri)
  }

  // entities

  trait CrmEntity

  case class CrmUser(
    uri: Uri,
    firstName: String,
    lastName: String,
    address: CrmAddress) extends CrmEntity

  case class CrmAddress(
    street1: String,
    street2: String,
    city: String,
    state: String,
    zipcode: Zipcode) extends CrmEntity

  case class CrmBlog(uri: Uri) extends CrmEntity

  case class CrmBlogPost(
    uri: Uri,

    authors: Set[CrmUser],
    comments: List[CrmComment],
    blog: Option[CrmBlog],

    tags: Set[String],
    longOpt: Option[Long],
    intList: List[Int])
  extends CrmEntity

  object CrmBlogPost {

    def apply(): CrmBlogPost = CrmBlogPost(
      "uri",
      Set(CrmUser("userUri")),
      List(CrmComment("c1"), CrmComment("c2")),
      Some(CrmBlog("blogUri")),
      Set("tag1", "tag2"),
      Some(0l),
      List(1, 2, 3))
  }

  case class CrmComment(uri: Uri) extends CrmEntity

  case class Email(email: String)

  case class Markdown(markdown: String)

  case class Uri(uri: String)

  case class Zipcode(zipcode: Int)

  // entity types

  trait CrmEntityType[E <: CrmEntity]
  object userType extends CrmEntityType[CrmUser]
  object blogType extends CrmEntityType[CrmBlog]

  // repos

  trait CrmRepo[E <: CrmEntity] {
    var saveCount = 0
    def save(entity: E): Unit = saveCount += 1
  }
  class CrmUserRepo extends CrmRepo[CrmUser]
  class CrmBlogRepo extends CrmRepo[CrmBlog]

}
