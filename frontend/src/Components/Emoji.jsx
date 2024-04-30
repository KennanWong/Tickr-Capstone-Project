import { Box, styled } from "@mui/material";
import { fontSize } from "@mui/system";
import React from "react";

export default function Emoji ({label, fontSize=20}) {
  const EmojiStyle = styled(Box)({
    height: '100%',
    fontSize: fontSize
  })

  var symbol = ''
  switch(label) {
    case 'heart':
      symbol = `\u2764\uFE0F`
      break
    case 'laugh':
      symbol = "😄"
      break
    case 'cry':
      symbol = "😥"
      break
    case 'angry':
      symbol = "😠"
      break
    case 'thumbs_up':
      symbol = "👍"
      break
    case 'thumbs_down':
      symbol = "👎"
      break
    default:
      symbol=''
      break
  }
  
  return (
    <EmojiStyle>
      {symbol}
    </EmojiStyle>
  )
}