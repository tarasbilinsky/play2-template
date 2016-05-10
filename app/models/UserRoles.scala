package models

import base.models.Lookup

trait UserRoles {
  private def generate(sysName: String) = Lookup.find(classOf[UserRole],sysName)
  val Admin = generate("Admin")
  val Dealer = generate("Dealer")


  private def generatePermission(sysName: String) = Lookup.find(classOf[Permission],sysName)
  val EditAll = generatePermission("EditAll")
  val ViewAll = generatePermission("ViewAll")

}
