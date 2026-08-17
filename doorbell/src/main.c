#include <stdio.h>
#include "lib/converter/converter.h"

int main(void) {
    printf("25C = %.2fF\n", celsiusToFahrenheit(25.0f));
    return 0;
}
