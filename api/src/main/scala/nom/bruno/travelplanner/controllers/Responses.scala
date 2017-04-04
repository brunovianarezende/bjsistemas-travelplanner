package nom.bruno.travelplanner.controllers

import org.{scalatra => s}

object Ok {
  def apply() = {
    s.Ok(Result(true, None, None))
  }

  def apply(data: Any) = {
    s.Ok(Result(true, Some(data), None))
  }
}

object NotFound {
  def apply(error: Error) = {
    s.NotFound(Result(false, None, Some(List(error))))
  }
}

object BadRequest {
  def apply(error: Error) = {
    s.BadRequest(Result(false, None, Some(List(error))))
  }
}

object Unauthorized {
  def apply(error: Error) = {
    s.Unauthorized(Result(false, None, Some(List(error))))
  }
}

object Forbidden {
  def apply(error: Error) = {
    s.Forbidden(Result(false, None, Some(List(error))))
  }
}