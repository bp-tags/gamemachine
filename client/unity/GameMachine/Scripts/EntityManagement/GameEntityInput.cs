﻿using UnityEngine;
using System.Collections;
using TrackData = io.gamemachine.messages.TrackData;
using UserDefinedData = io.gamemachine.messages.UserDefinedData;
using System;

namespace GameMachine {
    namespace Common {
        public class GameEntityInput : MonoBehaviour, IGameEntityInput {

            private ControllerType controllerType;
            private NetworkFields networkFields;
            private bool hidden = false;
            private IGameEntity gameEntity;

            void Start() {
                gameEntity = gameObject.GetComponent<IGameEntity>() as IGameEntity;
                networkFields = gameEntity.GetNetworkFields();
            }

            
            public void SetControllerType(ControllerType controllerType) {
                this.controllerType = controllerType;
            }

            public float GetFloat(GMKeyCode code) {
                switch (code) {
                    case GMKeyCode.Vaxis:
                        return Vaxis();
                    case GMKeyCode.Haxis:
                        return Haxis();
                    case GMKeyCode.Heading:
                        return Heading();
                    case GMKeyCode.AngleY:
                        return AngleY();
                    case GMKeyCode.PlayerSpeed:
                        return PlayerSpeed();
                    default:
                        throw new NotImplementedException("Invalid code");
                }
            }

            public int GetInt(GMKeyCode code) {
                throw new NotImplementedException();
            }

            public bool GetBool(GMKeyCode code) {
                 switch (code) {
                     case GMKeyCode.MouseRight:
                         return MouseRight();
                     case GMKeyCode.Jumping:
                        return Jumping();
                     case GMKeyCode.Hidden:
                         return Stealthed();
                    default:
                        throw new NotImplementedException("Invalid code");
                }
            }

            private float Heading() {
                if (controllerType == ControllerType.Local) {
                    Vector3 forward = transform.forward;
                    forward.y = 0;
                    return Quaternion.LookRotation(forward).eulerAngles.y;
                } else {
                    return networkFields.GetFloat(GMKeyCode.Heading);
                }
            }

            private float AngleY() {
                return networkFields.GetFloat(GMKeyCode.AngleY);
            }

            private bool Jumping() {
                if (controllerType == ControllerType.Local) {
                    if (!InputState.KeyInputActive() && Input.GetButtonDown("Jump")) {
                        networkFields.SetBool(GMKeyCode.Jumping, true);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return networkFields.GetBool(GMKeyCode.Jumping);
                }
            }

            private float PlayerSpeed() {
                if (controllerType == ControllerType.Local) {
                    return gameEntity.GetGameEntityController().GetPlayerSpeed();
                } else {
                    return networkFields.GetFloat(GMKeyCode.PlayerSpeed);
                }
            }

            private float Vaxis() {
                if (controllerType == ControllerType.Local) {
                    if (InputState.KeyInputActive()) {
                        return 0f;
                    } else {
                        return Input.GetAxis("Vertical");
                    }
                   
                } else {
                    return networkFields.GetFloat(GMKeyCode.Vaxis);
                }
            }

            private float Haxis() {
                if (controllerType == ControllerType.Local) {
                    if (InputState.KeyInputActive()) {
                        return 0f;
                    } else {
                        return Input.GetAxis("Horizontal");
                    }
                   
                } else {
                    return networkFields.GetFloat(GMKeyCode.Haxis);
                }
               
            }

            private bool MouseRight() {
                if (controllerType == ControllerType.Local) {
                    if (Input.GetMouseButton(1)) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return networkFields.GetBool(GMKeyCode.MouseRight);
                }
            }


            private bool RunWalkToggled() {
                if (controllerType == ControllerType.Local) {
                    return Input.GetKeyDown(KeyCode.R);
                } else {
                    return false;
                }
            }

            private bool Stealthed() {
                if (controllerType == ControllerType.Local) {
                    if (Input.GetKeyDown(KeyCode.LeftControl) || Input.GetKeyDown(KeyCode.RightControl)) {
                        if (hidden) {
                            networkFields.SetBool(GMKeyCode.Hidden, false);
                            hidden = false;
                        } else {
                            networkFields.SetBool(GMKeyCode.Hidden, true);
                            hidden = true;
                        }
                    }
                    return hidden;
                } else {
                    return networkFields.GetBool(GMKeyCode.Hidden);
                }
            }

            public string GetString(GMKeyCode code) {
                throw new NotImplementedException();
            }

            public Vector3 GetVector3(GMKeyCode code) {
                throw new NotImplementedException();
            }

            public Quaternion GetQuaternion(GMKeyCode code) {
                throw new NotImplementedException();
            }
        }
    }
}
