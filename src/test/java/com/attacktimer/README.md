# Testing Approach

Instead of all testing being done in game in the client and capturing the results as screen shots or videos,
we can instead programmatically mock out scenarios and concretely assert that the plugin state is as expected.

This can be done via the `performStateVerificationOrUpdate` function which will assert that the plugin state
and test messages are expected. These expectations can be created programmatically too, essentially marking
them as "golden" and verified to be working when they were added. This has better guarantees that more
unexpected state is asserted on, which will help reduce breakages. However it will also produce more
false-negatives as too much state is captured. This trade-off is worth it in my eyes.

Finally because the test expectations are human plain text, they're easy to digest and diff between revisions.

## How do I run the tests?

```
gradlew test
```

You should see something like:
```
--------------------------------------------------------------------
|  Results: SUCCESS (4 tests, 3 successes, 0 failures, 1 skipped)  |
--------------------------------------------------------------------
```

If all the tests are passing.

## How do I update test expectations?

```
gradlew test -Pupdate=all
```

You should now see a dirty working dir as the new expectations will be written to your disk.