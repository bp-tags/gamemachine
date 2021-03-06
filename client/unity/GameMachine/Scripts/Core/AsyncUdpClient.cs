﻿using System;
using System.Net;
using System.Net.Sockets;
using System.IO;
using ProtoBuf;
using System.Collections.Concurrent;
using System.Collections.Generic;
using io.gamemachine.messages;

namespace GameMachine.Core {
    public class AsyncUdpClient : Client
	{
		private IPEndPoint udp_ep;
		private UdpClient udpClient;
		private int port;
		private string host;
		private ClientMessage clientMessage;
		private string playerId;
		private int authtoken;
		public bool running = false;

        private GmSerializer serializer = new GmSerializer();
        private long bytesIn;
        private long bytesOut;

        private bool receivedPlayerConnected = false;

		// 0 = combined.  Single server setup where cluster/region is on one server, or where
		// you simply do not have regions.
		// 1 = region.  Region connection.
		// 2 = cluster.  Cluster connection
		private int connectionType = 0;
		public static ConcurrentQueue<Entity> entityQueue = new ConcurrentQueue<Entity> ();

		public AsyncUdpClient (string host, int port, string _playerId, int authtoken, bool measure=false)
		{
			playerId = _playerId;
			this.authtoken = authtoken;
			this.host = host;
			this.port = port;
			clientMessage = CreateClientMessage ();
            receivedPlayerConnected = false;
		}

		public bool IsRunning ()
		{
			return running;
		}

		public void SetConnectionType (int connectionType)
		{
			this.connectionType = connectionType;
		}

		public ClientMessage CreateClientMessage ()
		{
            Player player = new Player();
			player.id = playerId;
			player.authtoken = authtoken;
			ClientMessage clientMessage = new ClientMessage ();
			clientMessage.connection_type = connectionType;
			clientMessage.player = player;
			return clientMessage;
		}

		public void Stop ()
		{
			running = false;
			PlayerLogout logout = new PlayerLogout ();
			logout.authtoken = authtoken;
			logout.playerId = playerId;
			ClientMessage message = CreateClientMessage ();
			message.playerLogout = logout;
			byte[] bytes = Serialize (message);
			udpClient.Send (bytes, bytes.Length, host, port);
			udpClient.Close ();
		}

		public void Start ()
		{
			running = true;
			udp_ep = new IPEndPoint (IPAddress.Any, 0);
			udpClient = new UdpClient (udp_ep);
			receiveData ();

			ClientMessage message = CreateClientMessage ();
			message.playerConnect = new PlayerConnect();
			Send (Serialize (message));
		}
			
        public void Reconnect() {
            UnityEngine.Debug.Log("Reconnect");
            udp_ep = new IPEndPoint(IPAddress.Any, 0);
            udpClient = new UdpClient(udp_ep);
            receiveData();
        }	

		private void SendCallback (IAsyncResult ar)
		{
			UdpClient u = (UdpClient)ar.AsyncState;
			u.EndSend (ar);
		}

		public void SendEntity (Entity entity)
		{
			clientMessage.entity.Add (entity);
			Send (Serialize (clientMessage));
			clientMessage.entity.Clear ();
		}

		public void SendEntities (List<Entity> entities)
		{
			foreach (Entity entity in entities) {
				clientMessage.entity.Add (entity);
			}

			Send (Serialize (clientMessage));
			clientMessage.entity.Clear ();
		}

		public void Send (byte[] bytes)
		{
            bytesOut += bytes.Length;
			udpClient.BeginSend (bytes, bytes.Length, host, port, new AsyncCallback (SendCallback), udpClient);
		}
		
		private void dataReady (IAsyncResult ar)
		{
            try {
                byte[] bytes = udpClient.EndReceive(ar, ref udp_ep);
                bytesIn += bytes.Length;
                ClientMessage message = Deserialize(bytes);

                if (receivedPlayerConnected) {
                    if (message.unityGameMessage != null) {
                        ClientMessageQueue.unityGameMessageQueue.Enqueue(message.unityGameMessage);
                    } else {
                        foreach (Entity entity in message.entity) {
                            ClientMessageQueue.entityQueue.Enqueue(entity);
                        }
                    }

                } else {
                    if (message.playerConnected != null) {
                        receivedPlayerConnected = true;
                    }
                }
            } catch (Exception e) {
                UnityEngine.Debug.LogWarning("UDP error "+e.Message);
            }
			
			
			receiveData ();
		}
		
		public void receiveData ()
		{
			udpClient.BeginReceive (new AsyncCallback (dataReady), udp_ep);
		}

		private ClientMessage Deserialize (byte[] bytes)
		{
			MemoryStream stream = new MemoryStream (bytes);
            return serializer.Deserialize(stream, new ClientMessage(), typeof(ClientMessage)) as ClientMessage;
			//return Serializer.Deserialize<ClientMessage> (stream);
		}
		
		private byte[] Serialize (ClientMessage message)
		{
			MemoryStream stream = new MemoryStream ();
            serializer.Serialize(stream, message);
			//Serializer.Serialize (stream, message);
			return stream.ToArray ();
		}


        public bool ReceivedPlayerConnected() {
            return receivedPlayerConnected;
        }


        public void SendPlayerConnect() {
            ClientMessage message = CreateClientMessage();
            message.playerConnect = new PlayerConnect();
            Send(Serialize(message));
        }

        public long GetBytesIn() {
            return bytesIn;
        }

        public long GetBytesOut() {
            return bytesOut;
        }

        public void ResetBytes() {
            bytesIn = 0L;
            bytesOut = 0L;
        }
    }
}