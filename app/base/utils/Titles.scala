package base.utils

object Titles {

  def camelCaseToTitleCase(x: String) = x.charAt(0).toUpper+x.substring(1)
  def camelCaseToTitle(x: String) = camelCaseToTitleCase(x)
    .replaceAll(
        String.format("%s|%s|%s",
          "(?<=[A-Z])(?=[A-Z][a-z])",
          "(?<=[^A-Z])(?=[A-Z])",
          "(?<=[A-Za-z])(?=[^A-Za-z])"
        ),
        " "
    )

}
