import React from 'react'
import { styled } from '@mui/system';
import logo from '../Images/TickrLogo.png'
import { Link } from "react-router-dom";

export default function StandardLogo({width = '100px', height='auto'}) {
  const Logo = styled('img')({
    width: width,
    height: height,
    position: 'fixed',
    padding: '20px',
  })

  return (
    <Link to="/">
      <Logo src={logo}/>
    </Link> 
  )
} 
