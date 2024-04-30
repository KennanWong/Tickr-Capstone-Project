import React from 'react'
import { styled } from '@mui/system';
import { Link } from "react-router-dom";
import logo from '../Images/TickrLogo.png'

export default function AdjustableLogo({width = '100px', height='auto'}) {
  const Logo = styled('img')({
    width: width,
    height: height,
  })

  return (
    <Link to="/">
      <Logo src={logo}/>
    </Link> 
  )
} 