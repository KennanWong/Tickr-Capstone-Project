import { IconButton, alpha } from "@mui/material"
import { Box } from "@mui/system"
import React from "react"
import { apiFetch, getToken } from "../Helpers"
import { CentredBox } from "../Styles/HelperStyles"
import Emoji from "./Emoji"

export default function ReactBar({comment_id, onReact}) {

  const handleReact = async (react_type) => {
    const body = {
      auth_token: getToken(),
      comment_id: comment_id,
      react_type: react_type
    }
    try {
      const response = await apiFetch('POST', '/api/event/review/react', body)
      onReact()
    } catch (e) {
      console.log(e)
    }
  }

  return (
    <Box sx={{backgroundColor: alpha('#6A7B8A', 0.3), borderRadius: 30, height: 30, p: 1, pr: 2, pl: 2}} >
      <CentredBox sx={{gap: 1}}>
        <IconButton sx={{p: '3px', color: 'rgba(0,0,0,1)'}} onClick={(e) => {handleReact('laugh')}}>
          <Emoji label="laugh"/>
        </IconButton>
        <IconButton sx={{p: '3px', color: 'rgba(0,0,0,1)'}} onClick={(e) => {handleReact('cry')}}>
          <Emoji label="cry"/>
        </IconButton>
        <IconButton sx={{p: '3px', color: 'rgba(0,0,0,1)'}} onClick={(e) => {handleReact('heart')}}>
          <Emoji label="heart"/>
        </IconButton>
        <IconButton sx={{p: '3px', color: 'rgba(0,0,0,1)'}} onClick={(e) => {handleReact('angry')}}>
          <Emoji label="angry"/>
        </IconButton>
        <IconButton sx={{p: '3px', color: 'rgba(0,0,0,1)'}} onClick={(e) => {handleReact('thumbs_up')}}>
          <Emoji  label="thumbs_up"/>
        </IconButton>
        <IconButton sx={{p: '3px', color: 'rgba(0,0,0,1)'}} onClick={(e) => {handleReact('thumbs_down')}}>
          <Emoji label="thumbs_down"/>
        </IconButton>
      </CentredBox>
    </Box>
  )
}