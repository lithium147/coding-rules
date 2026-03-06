
# TODO create syntactic sugar library

# TODO's

## collect to set should normally be replace with toUnmodifiableSet()
eg:
                .collect(Collectors.toSet());
replace with:
                .collect(toUnmodifiableSet());

