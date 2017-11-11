#!/bin/bash -e

source "experiments/dipart10_9/scripts/config.sh"

MY_NAME=matching
MY_DIR=$EXPERIMENT_DIR/$MY_NAME/
MY_MODEL=$MY_DIR/model.ser
MY_FLAGS="--matchingNetwork --matchIndependent --loglikelihood"
MY_EPOCHS=5

TEST_BEAM="100"

MY_NAME_TEST=matching_beam$TEST_BEAM
TEST_DIR=$EXPERIMENT_DIR/$MY_NAME_TEST/

mkdir -p $MY_DIR
mkdir -p $TEST_DIR

#echo "Training $MY_NAME model..."
#./$SCRIPT_DIR/run.sh org.allenai.dqa.matching.TrainMatchingCli --beamSize $TRAIN_BEAM --epochs $MY_EPOCHS --examples $TRAIN --diagrams $DIAGRAMS --diagramFeatures $DIAGRAM_FEATURES --modelOut $MY_MODEL $TRAIN_OPTS $MY_FLAGS > $MY_DIR/log.txt

echo "Testing $MY_NAME model on test..."
TEST="$DATA_DIR/data_splits/$DATA_SPLIT/test.json"
./$SCRIPT_DIR/run.sh org.allenai.dqa.matching.TestMatchingCli --beamSize $TEST_BEAM --examples $TEST --diagrams $DIAGRAMS --diagramFeatures $DIAGRAM_FEATURES --model $MY_MODEL --lossJson $TEST_DIR/test_error_independent.json  > $TEST_DIR/test_error_independent_log.txt

./$SCRIPT_DIR/run.sh org.allenai.dqa.matching.TestMatchingCli --beamSize 120 --enforceMatching --globalNormalize --examples $TEST --diagrams $DIAGRAMS --diagramFeatures $DIAGRAM_FEATURES --model $MY_MODEL --lossJson $TEST_DIR/test_error_matching.json  > $TEST_DIR/test_error_matching_log.txt

echo "Testing $MY_NAME model on validation..."
VAL="$DATA_DIR/data_splits/$DATA_SPLIT/validation.json"
./$SCRIPT_DIR/run.sh org.allenai.dqa.matching.TestMatchingCli --beamSize $TEST_BEAM --examples $VAL --diagrams $DIAGRAMS --diagramFeatures $DIAGRAM_FEATURES --model $MY_MODEL --lossJson $TEST_DIR/validation_error_independent.json  > $TEST_DIR/validation_error_independent_log.txt

./$SCRIPT_DIR/run.sh org.allenai.dqa.matching.TestMatchingCli --beamSize 120 --enforceMatching --globalNormalize --examples $VAL --diagrams $DIAGRAMS --diagramFeatures $DIAGRAM_FEATURES --model $MY_MODEL --lossJson $TEST_DIR/validation_error_matching.json  > $TEST_DIR/validation_error_matching_log.txt


echo "Finished training $MY_NAME"

