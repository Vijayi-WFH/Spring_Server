'use client';

import { Button, Input, InputLabel, MenuItem, Select } from '@mui/material';
import { useEffect, useRef, useState } from 'react';

const base = 'ws://wfh-tech.in';

const ChatRoom = () => {
  const [messages, setMessages] = useState<{ senderId: string; content: string; attachments?: string[] }[]>([]);
  const [input, setInput] = useState('');
  const [receiverId, setReceiverId] = useState('');
  const [action, setAction] = useState('');
  const [messageId, setMessageId] = useState('');
  const [attachments, setAttachments] = useState<File[]>([]);
  const wsRef = useRef<WebSocket | null>(null);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const [isSending, setIsSending] = useState(false); // Track if message is being sent

  useEffect(() => {
    const userId = sessionStorage.getItem("userId");
    if (!userId) {
      console.error("No userId found in sessionStorage.");
      return;
    }
    wsRef.current = new WebSocket(`${base}/chat?userId=${userId}`);

    wsRef.current.onopen = () => {
      console.log("WebSocket connection established.");
    };

    wsRef.current.onmessage = (event) => {
      const message = JSON.parse(event.data);
      console.log("Received message:", message);
      setMessages((prev) => [...prev, message]);
      scrollToBottom();
    };

    wsRef.current.onerror = (error) => {
      console.error("WebSocket error:", error);
    };

    wsRef.current.onclose = (event) => {
      console.log("WebSocket connection closed:", event.code, event.reason);
    };

    return () => {
      if (wsRef.current) {
        wsRef.current.close();
        console.log("WebSocket connection manually closed.");
      }
    };
  }, []);

  const sendMessage = () => {
    if (input.trim() !== "" && wsRef.current) {
      if (wsRef.current.readyState !== WebSocket.OPEN) {
        console.error("WebSocket is not open. Cannot send message.");
        return; // Early return if WebSocket is not open
      }

      setIsSending(true); // Mark as sending

      const messageData: any = { content: input, action };

      if (action === 'NEW' || action === 'TAG' || action === 'EDIT') {
        messageData.receiverId = receiverId;
      }
      if (action === 'GROUP') {
        messageData.groupId = receiverId;
      }
      if (action === 'TAG' || action === 'EDIT' || action === 'DELETE') {
        messageData.messageId = messageId;
      }

      if (attachments.length > 0) {
        sendFileInChunks(attachments[0]); // Send only the first attachment for simplicity
      } else {
        console.log("Sending message:", messageData);
        wsRef.current.send(JSON.stringify(messageData));
        setIsSending(false); // Reset sending state
      }

      setInput(''); // Clear the input after sending
      setMessageId(''); // Clear messageId if applicable
    } else {
      console.error("Message input is empty or WebSocket is not open.");
    }
  };

  const sendFileInChunks = async (file: File) => {
    const chunkSize = 4 * 1024; // Set chunk size to 32 KB
    const totalChunks = Math.ceil(file.size / chunkSize); // Calculate the total number of chunks
    console.log(`Sending file ${file.name} in ${totalChunks} chunks`);

    // Loop to send the file in smaller chunks
    for (let i = 0; i < totalChunks; i++) {
      const chunk = file.slice(i * chunkSize, (i + 1) * chunkSize); // Slice the file into smaller chunks
      const base64Data = await readFileAsBase64(chunk); // Read the chunk as base64

      // Send the chunk if the WebSocket is open
      if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
        const messageData = {
          action: 'upload',
          chunk: base64Data,
          chunkIndex: i,
          totalChunks: totalChunks,
          fileName: file.name,
        };

        console.log(`Sending chunk ${i + 1}/${totalChunks}:`, messageData);
        wsRef.current.send(JSON.stringify(messageData)); // Send chunk over WebSocket
      } else {
        console.error("WebSocket is not open. Current state:", wsRef.current?.readyState);
        break; // Stop sending if the WebSocket is not open
      }
    }

    // Notify that the file transfer is complete after all chunks are sent
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ action: 'upload_complete', fileName: file.name }));
    }

    // Clear attachments after sending
    setAttachments([]);
  };

  const readFileAsBase64 = (file: Blob): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onloadend = () => resolve(reader.result as string);
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  return (
    <div>
      <div className="border-black border-2 p-5 flex flex-col items-center justify-end h-screen w-3/4">
        <div id="messages" className="h-40 w-full mb-5" style={{ overflowY: 'scroll' }}>
          {messages.map((message, index) => (
            <div key={index} className="mb-4 flex items-center space-x-2">
              <div className="flex-grow">
                <strong>{message.senderId}:</strong> {message.content}
              </div>
              {message.attachments && message.attachments.length > 0 && (
                <div className="flex space-x-2">
                  {message.attachments.map((attachment, idx) => (
                    <div key={idx} className="mt-2 flex items-center">
                      {/* Display attachment as a download link */}
                      <a
                        href={attachment} // Assuming attachment is the URL or file path
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-blue-500"
                      >
                        Download Attachment {idx + 1}
                      </a>

                      {/* If it's an image, display it inline */}
                      {attachment.match(/\.(jpeg|jpg|gif|png)$/i) && (
                        <img
                          src={attachment}
                          alt={`attachment-${idx + 1}`}
                          style={{ maxWidth: '100px', marginLeft: '10px' }}
                        />
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
          <div ref={messagesEndRef} />
        </div>

        <div className="w-full">
          <InputLabel id="action-label">Action</InputLabel>
          <Select
            className="w-1/6"
            labelId="action-label"
            id="action-select"
            value={action}
            onChange={(e) => setAction(e.target.value)}
          >
            <MenuItem value="NEW">One-to-one</MenuItem>
            <MenuItem value="GROUP">Group</MenuItem>
            <MenuItem value="TAG">Tag</MenuItem>
            <MenuItem value="EDIT">Edit</MenuItem>
            <MenuItem value="DELETE">Delete</MenuItem>
          </Select>

          <Input
            className="w-1/6 bg-slate-100"
            type="text"
            placeholder="Receiver ID"
            value={receiverId}
            onChange={(e) => setReceiverId(e.target.value)}
          />
          <Input
            className="w-3/6"
            type="text"
            placeholder="Enter your message"
            value={input}
            onChange={(e) => setInput(e.target.value)}
          />
          {(action === 'edit' || action === 'delete' || action === 'tag') && (
            <Input
              type="text"
              placeholder="Message ID"
              value={messageId}
              onChange={(e) => setMessageId(e.target.value)}
            />
          )}

          <Input
            type="file"
            onChange={(e: any) => {
              if (e.target.files) {
                setAttachments(Array.from(e.target.files)); // Convert FileList to array
              }
            }}
          />

          <Button
            className="w-1/6"
            onClick={sendMessage}
            disabled={isSending}
          >
            Send
          </Button>
        </div>
      </div>
    </div>
  );
};

export default ChatRoom;
