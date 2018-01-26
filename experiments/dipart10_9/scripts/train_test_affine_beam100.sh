#!/bin/bash -e

source "experiments/dipart10_9/scripts/config.sh"

TEST_BEAM="100"

MY_NAME=affine_beam$TEST_BEAM
MY_DIR=$EXPERIMENT_DIR/$MY_NAME/
MY_MODEL=$MY_DIR/model.ser
MY_FLAGS="--affineTransform"

MY_NAME_TEST=affine_beam$TEST_BEAM
TEST_DIR=$EXPERIMENT_DIR/$MY_NAME_TEST/

mkdir -p $MY_DIR
mkdir -p $TEST_DIR

echo "Training $MY_NAME model..."
./$SCRIPT_DIR/run.sh org.allenai.dqa.matching.TrainMatchingCli --beamSize $TRAIN_BEAM --epochs $EPOCHS --examples $TRAIN --diagrams $DIAGRAMS --diagramFeatures $DIAGRAM_FEATURES --modelOut $MY_MODEL $TRAIN_OPTS $MY_FLAGS > $MY_DIR/log.txt

#echo "Testing $MY_NAME model on validation..."
#VAL="$DATA_DIR/data_splits/$DATA_SPLIT/validation.json"
#./$SCRIPT_DIR/run.sh org.allenai.dqa.matching.TestMatchingCli --beamSize 120 --examples $VAL --diagrams $DIAGRAMS --diagramFeatures $DIAGRAM_FEATURES --model $MY_MODEL --lossJson $MY_DIR/validation_error.json  > $MY_DIR/validation_error_log.txt

echo "Testing $MY_NAME model on test..."
TEST="$DATA_DIR/data_splits/$DATA_SPLIT/test.json"
./$SCRIPT_DIR/run.sh org.allenai.dqa.matching.TestMatchingCli --beamSize $TEST_BEAM --examples $TEST --diagrams $DIAGRAMS --diagramFeatures $DIAGRAM_FEATURES --model $MY_MODEL --lossJson $TEST_DIR/test_error.json > $TEST_DIR/test_error_log.txt


# ./$SCRIPT_DIR/run.sh org.allenai.dqa.matching.TestMatchingCli --beamSize 120 --examples $TEST --diagrams $DIAGRAMS --diagramFeatures $DIAGRAM_FEATURES --model $MY_MODEL --lossJson $MY_DIR/test_error.json  > $MY_DIR/test_error_log.txt

# ./$SCRIPT_DIR/run.sh org.allenai.dqa.matching.TestMatchingCli --beamSize $TEST_BEAM --examples $TEST --diagrams $DIAGRAMS --diagramFeatures $DIAGRAM_FEATURES --model $MY_MODEL --lossJson $MY_DIR/test_error.json  > $MY_DIR/test_error_log.txt

# mkdir -p $MY_DIR/test_error/
# python $SCRIPT_DIR/visualize/visualize_loss.py $MY_DIR/test_error.json $MY_DIR/test_error/
# tar cf $MY_DIR/test_error.tar $MY_DIR/test_error/
# gzip -f $MY_DIR/test_error.tar

# /$SCRIPT_DIR/run.sh org.allenai.dqa.matching.TestMatchingCli --beamSize $TEST_BEAM --examples $TRAIN --diagrams $DIAGRAMS --diagramFeatures $DIAGRAM_FEATURES --model $MY_MODEL --lossJson $MY_DIR/train_error.json  > $MY_DIR/train_error_log.txt

echo "Finished training $MY_NAME"
