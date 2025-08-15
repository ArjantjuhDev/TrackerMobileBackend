// Realtime backend met Socket.io
const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const cors = require('cors');

const app = express();
const server = http.createServer(app);
const io = socketIo(server, { cors: { origin: '*' } });

app.use(cors());
app.get('/', (req, res) => res.send('Realtime server running'));

// Koppeling: unieke room per sessie
io.on('connection', (socket) => {
  console.log('Client connected:', socket.id);

  socket.on('join-room', (room) => {
    socket.join(room);
    socket.to(room).emit('user-joined', socket.id);
  });

  // Data doorsturen (bijv. schermdata, commando's)
  socket.on('screen-data', (room, data) => {
    socket.to(room).emit('screen-data', data);
  });

  socket.on('disconnect', () => {
    console.log('Client disconnected:', socket.id);
  });
});

const PORT = process.env.PORT || 4000;
server.listen(PORT, () => console.log(`Socket.io server running on port ${PORT}`));
