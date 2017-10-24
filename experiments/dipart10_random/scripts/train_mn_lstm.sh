#!/bin/bash -e

source "experiments/dipart10_random/scripts/config.sh"

MY_NAME=matching_lstm2_bugfixed
MY_DIR=$EXPERIMENT_DIR/$MY_NAME/
MY_MODEL=$MY_DIR/model.ser
MY_FLAGS="--lstmEncode --matchIndependent --loglikelihood"
MY_EPOCHS=5

mkdir -p $MY_DIR

echo "Training $MY_NAME model..."
./$SCRIPT_DIR/run.sh org.allenai.dqa.matching.TrainMatchingCli --beamSize $TRAIN_BEAM --epochs $MY_EPOCHS --examples $TRAIN --diagrams $DIAGRAMS --diagramFeatures $DIAGRAM_FEATURES --modelOut $MY_MODEL $TRAIN_OPTS $MY_FLAGS > $MY_DIR/log.txt

echo "Testing $MY_NAME model on validation..."
VAL="$DATA_DIR/data_splits/$DATA_SPLIT/validation.json"
./$SCRIPT_DIR/run.sh org.allenai.dqa.matching.TestMatchingCli --beamSize $TEST_BEAM --examples $VAL --diagrams $DIAGRAMS --diagramFeatures $DIAGRAM_FEATURES --model $MY_MODEL --lossJson $MY_DIR/validation_error_independent.json  > $MY_DIR/validation_error_independent_log.txt

./$SCRIPT_DIR/run.sh org.allenai.dqa.matching.TestMatchingCli --beamSize 120 --enforceMatching --globalNormalize --examples $VAL --diagrams $DIAGRAMS --diagramFeatures $DIAGRAM_FEATURES --model $MY_MODEL --lossJson $MY_DIR/validation_error_matching.json  > $MY_DIR/validation_error_matching_log.txt


echo "Testing $MY_NAME model on test..."
TEST="$DATA_DIR/data_splits/$DATA_SPLIT/test.json"
./$SCRIPT_DIR/run.sh org.allenai.dqa.matching.TestMatchingCli --beamSize $TEST_BEAM --examples $TEST --diagrams $DIAGRAMS --diagramFeatures $DIAGRAM_FEATURES --model $MY_MODEL --lossJson $MY_DIR/test_error_independent.json  > $MY_DIR/test_error_independent_log.txt

./$SCRIPT_DIR/run.sh org.allenai.dqa.matching.TestMatchingCli --beamSize 120 --enforceMatching --globalNormalize --examples $TEST --diagrams $DIAGRAMS --diagramFeatures $DIAGRAM_FEATURES --model $MY_MODEL --lossJson $MY_DIR/test_error_matching.json  > $MY_DIR/test_error_matching_log.txt


# mkdir -p $MY_DIR/validation_error_matching/
# python $SCRIPT_DIR/visualize/visualize_loss.py $MY_DIR/validation_error_matching.json $MY_DIR/validation_error_matching/
# tar cf $MY_DIR/validation_error_matching.tar $MY_DIR/validation_error_matching/
# gzip -f $MY_DIR/validation_error_matching.tar

# ./$SCRIPT_DIR/run.sh org.allenai.dqa.matching.TestMatchingCli --beamSize $TEST_BEAM --examples $TRAIN --diagrams $DIAGRAMS --diagramFeatures $DIAGRAM_FEATURES --model $MY_MODEL --lossJson $MY_DIR/train_error_independent.json  > $MY_DIR/train_error_independent_log.txt

# ./$SCRIPT_DIR/run.sh org.allenai.dqa.matching.TestMatchingCli --beamSize 120 --enforceMatching --globalNormalize --examples $TRAIN --diagrams $DIAGRAMS --diagramFeatures $DIAGRAM_FEATURES --model $MY_MODEL --lossJson $MY_DIR/train_error_matching.json  > $MY_DIR/train_error_matching_log.txt

# mkdir -p $MY_DIR/train_error_matching/
# python $SCRIPT_DIR/visualize/visualize_loss.py $MY_DIR/train_error_matching.json $MY_DIR/train_error_matching/
# tar cf $MY_DIR/train_error_matching.tar $MY_DIR/train_error_matching/
# gzip -f $MY_DIR/train_error_matching.tar

echo "Finished training $MY_NAME"

