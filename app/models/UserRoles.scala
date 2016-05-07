package models

import base.models.Lookup

trait UserRoles {
  private def generate(sysName: String) = Lookup.find(classOf[UserRole],sysName)
  val Admin = generate("Admin")
  val Manager = generate("Manager")
  //TODO create macro to remove boilerplate
  /*
  *
  * import macro.generate
  * val Admin = generate
  *
   */

  private def generatePermission(sysName: String) = Lookup.find(classOf[Permission],sysName)
  val EditAll = generatePermission("EditAll")
  val ViewAll = generatePermission("ViewAll")

}
