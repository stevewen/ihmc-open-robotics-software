# This message is part of the IHMC whole-body controller API.
# This class is used to build trajectory messages in jointspace 
# It holds all the trajectory points to go through with a one-dimensional trajectory.
# A third order polynomial function is used to interpolate between trajectory points.

# Unique ID used to identify this message, should preferably be consecutively increasing.
uint32 sequence_id

# The list of trajectory points to go through while executing the trajectory.
# The time has to be strictly increasing.
TrajectoryPoint1DMessage[<=50] trajectory_points
# Weight used to encode the priority for achieving this trajectory:
# - if too low, in the event the controller can't achieve all of the objectives it may lower the trajectory tracking quality. 
# - if too high, the controller will favor this trajectory over other objectives.
# - if set to NaN or to a negative value, the controller will use the default weight for that trajectory.
# The priority of this trajectory is determined from the relative weight of this trajectory and the weight of the other objectives.
float64 weight -1