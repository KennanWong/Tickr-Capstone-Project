import { Typography } from "@mui/material"
import { Box } from "@mui/system"
import React from "react"
import Emoji from "./Emoji"

export default function ReactionsList ({reactions, size=25}) {
  if (reactions !== undefined) {
    if ((reactions.length) > 0) {
      return (
        <Box sx={{display: 'flex', gap: 1, alignItems: 'center', width: 'fit-content'}}>
          {reactions.map((reaction, key) => {
            return (
              <Box key={key} sx={{display: 'flex', height: '100%', alignItems: 'center', p: '3px', borderRadius: 3, backgroundColor: '#BBBBBB'}}>
                <Emoji label={reaction.react_type} fontSize={size}/>
                <Typography>
                  {reaction.react_num}
                </Typography>
              </Box>
            )
          })
  
          }
        </Box>
      )
    }
  }
}