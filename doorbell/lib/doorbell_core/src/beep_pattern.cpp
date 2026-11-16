#include "beep_pattern.h"

namespace beep_pattern {

const BeepPattern kPressAccepted = {{100}, 1};
const BeepPattern kDelivered = {{80, 80, 80}, 3};
const BeepPattern kLost = {{600}, 1};

const BeepPattern& forSendResult(int httpStatus) {
  bool delivered = httpStatus >= 200 && httpStatus < 300;
  return delivered ? kDelivered : kLost;
}

}  // namespace beep_pattern
