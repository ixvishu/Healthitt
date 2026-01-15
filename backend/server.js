const express = require('express');
const nodemailer = require('nodemailer');
const cors = require('cors');
const bodyParser = require('body-parser');

const app = express();
app.use(cors());
app.use(bodyParser.json());

// IMPORTANT: Replace these with your actual Gmail and App Password
// To get an App Password: https://myaccount.google.com/apppasswords
const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: 'your-email@gmail.com',
    pass: 'your-app-password'
  }
});

app.post('/send-otp', (req, { email, otp }) => {
  const mailOptions = {
    from: 'Healthitt <your-email@gmail.com>',
    to: email,
    subject: 'Verification Code for Healthitt',
    text: `Your 4-digit verification code is: ${otp}. Please enter this in the app to complete your registration.`
  };

  transporter.sendMail(mailOptions, (error, info) => {
    if (error) {
      console.log('Error:', error);
      res.status(500).send('Failed to send OTP');
    } else {
      console.log('Email sent: ' + info.response);
      res.status(200).send('OTP sent successfully');
    }
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
