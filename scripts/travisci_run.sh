#!/bin/sh
set -eux

# We export TERM=dumb to hide Buck's SuperConsole
# because otherwise Travis thinks our logs are too long.
export TERM=dumb

# Use Buck to build first because its output is more
# conducive to diagnosing failed builds than Ant's is.
./bin/buck build buck || { cat buck-out/log/ant.log; exit 1; }

# There are only two cores in the containers, but Buck thinks there are 12
# and tries to use all of them.  As a result, we seem to have our test
# processes killed if we do not limit our threads.
export BUCK_NUM_THREADS=3

if [ "$CI_ACTION" = "build" ]; then
  # Make sure that everything builds in case a library is not covered by a test.
  ./bin/buck build --num-threads=$BUCK_NUM_THREADS src/... test/...
fi

if [ "$CI_ACTION" = "unit" ]; then
  ./bin/buck test --num-threads=$BUCK_NUM_THREADS --all --test-selectors '!.*[Ii]ntegration.*'
fi

if [ "$CI_ACTION" = "integration" ]; then
  ./bin/buck test --num-threads=$BUCK_NUM_THREADS --all --filter '^(?!(com.facebook.buck.android|com.facebook.buck.jvm.java)).*[Ii]ntegration.*'
fi

if [ "$CI_ACTION" = "heavy_integration" ]; then
  ./bin/buck build --num-threads=$BUCK_NUM_THREADS //test/com/facebook/buck/android/... //test/com/facebook/buck/jvm/java/...
  ./bin/buck test  --num-threads=1 //test/com/facebook/buck/android/... //test/com/facebook/buck/jvm/java/... --filter '.*[Ii]ntegration.*'
fi

if [ "$CI_ACTION" = "ant" ]; then
  # Run all the other checks with ant.
  ant travis
fi
