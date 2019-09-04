/* Copyright 2017-2018 All Rights Reserved.
 *  Gyeonghwan Hong (redcarrottt@gmail.com)
 *
 * [Contact]
 *  Gyeonghwan Hong (redcarrottt@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0(the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef __BT_SERVER_ADAPTER_H__
#define __BT_SERVER_ADAPTER_H__

#include "BtDevice.h"
#include "BtP2PServer.h"
#include "RfcommServerSocket.h"

#include "../../core/inc/ServerAdapter.h"

#include "../../configs/ExpConfig.h"

#include <mutex>
#include <thread>

#include <stdio.h>

namespace sc {
class BtServerAdapter : public ServerAdapter {
public:
  ~BtServerAdapter(void) {}

  /* Singleton */
  static BtServerAdapter *singleton(int id, const char *name,
                                       const char *service_uuid) {
    if (sSingleton == NULL) {
      sSingleton = new BtServerAdapter(id, name, service_uuid);
    }
    return sSingleton;
  }

  virtual void prepare_switch(int prev_id, int next_id) {
    if(prev_id == this->get_id()) {
      // Prepare switch off
      this->mServerSocket->set_threshold_switch();
    } else if(next_id == this->get_id()) {
      // Prepare switch on
      this->mServerSocket->set_threshold_normal();
    }
  }

  virtual void cancel_preparing_switch(int prev_id, int next_id) {
    if(prev_id == this->get_id()) {
      // Cancel prepare switch off
      this->mServerSocket->set_threshold_normal();
    } else if(next_id == this->get_id()) {
      // Cancel prepare switch on
      this->mServerSocket->set_threshold_normal();
    }
  }

private:
  /* Singleton */
  static BtServerAdapter *sSingleton;

  /* Components */
  RfcommServerSocket *mServerSocket;

  /* Constructor */
  BtServerAdapter(int id, const char *name, const char *service_uuid)
      : ServerAdapter(id, name) {
    BtDevice *device = new BtDevice();
    BtP2PServer *p2pServer = new BtP2PServer();
    this->mServerSocket =
        new RfcommServerSocket(name, service_uuid, this);
    this->initialize(device, p2pServer, this->mServerSocket);
  }
}; /* class BtServerAdapter */
} /* namespace sc */

#endif /* !defined(__BT_SERVER_ADAPTER_H__) */