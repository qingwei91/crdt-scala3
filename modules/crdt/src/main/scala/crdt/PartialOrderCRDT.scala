package crdt

/** This trait expresses the idea of Add-Remove Partial Ordered Datatype described in
  * https://hal.inria.fr/inria-00555588/document
  *
  * It is a multi-value data type, ie. a collection of data, it supports adding a value into the
  * collection and removal of the value.
  *
  * It is convergent iff there exists a partial order of the value type `A`, when A has a partial
  * order, there are a few concurrent scenario that we need to consider:
  *
  *   1. Concurrent Add and Remove of the same value
  *   1. Concurrent add of multiple values that depends on the same prior value
  *   1. Concurrent add and removal of values with dependency
  *
  * The 1st case is solved by 2 rules:
  *
  *   1. Only execute remove if a value has been added.
  *   1. Values should be unique, for example 2 at T0 and 2 and T2 should not be considered equal in
  *      this context.
  *
  * The 2nd and 3rd case are solved by partial order (or total order depending on implementation).
  * If there exists a partial order, then we know that which value will come next even if a prior
  * element has been modified concurrently. For example, given 1 -> 2 -> 5 -> 6, if we concurrently
  * remove 2 and 5, we know that 6 should follow 1 because or partial ordering.
  *
  * Note that while Partial Order is useful to reason about the behavior, in reality, we dont
  * necessarily use [[scala.math.PartialOrdering]] directly, because many data structure has
  * Partial(or total) ordering directly encoded, eg. List, Array etc
  *
  * I am not able to encode this into type level, so it is the implementors responsibility to ensure
  * the rules are correct.
  */
trait PartialOrderCRDT[Col[_, _], Key, Value] extends CmRDT[Col[Key, Value]] {

  sealed trait PartialOrderOps
  case class InsertAfterA(anchor: Key, value: Value) extends PartialOrderOps
  case class Remove(key: Key)                        extends PartialOrderOps

  override type LocalOp  = PartialOrderOps
  override type RemoteOp = PartialOrderOps

  def insertXAfterA(col: Col[Key, Value], anchor: Key, addedValue: Value): Col[Key, Value]
  def remove(col: Col[Key, Value], toRemove: Key): Col[Key, Value]

  extension (ca: Col[Key, Value]) {
    def syncRemote(op: RemoteOp): Col[Key, Value] = ca.change(op)._2

    def change(op: LocalOp): (RemoteOp, Col[Key, Value]) = op match {
      case InsertAfterA(anchor, value) => op -> this.insertXAfterA(ca, anchor, value)
      case Remove(value)               => op -> this.remove(ca, value)
    }
  }
}
