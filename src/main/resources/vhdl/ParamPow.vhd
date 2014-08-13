

library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

-- For Modelsim
--use ieee.fixed_pkg.all;
--use ieee.fixed_float_types.ALL;

-- For ISE
library ieee_proposed;
use ieee_proposed.fixed_pkg.all;
use ieee_proposed.fixed_float_types.ALL;
use IEEE.numeric_std.all;

entity ParamPow is
	port(
		clk		: In  Std_logic;
		rst		: In  Std_logic;
		Start	: In  Std_logic;
		Done	: Out  Std_logic;
		A		: In sfixed(11 downto -12);
		X		: In sfixed(11 downto -12);
		Output	: Out sfixed(11 downto -12)
		);
end ParamPow; 

architecture RTL of ParamPow is
signal output_internal : sfixed(11 downto -12);
signal count : sfixed(11 downto -12);
begin
	 
	process(clk)
		variable Sel : integer;
		begin
			if rst = '1' then
				count <= to_sfixed(1,11,-12);
				output_internal <= to_sfixed (0,11, -12);
			elsif clk'event and clk = '1' then
				if Start = '1' then
					count <= to_sfixed(1,11,-12);
					output_internal <= A;
						Done <= '0';
				else
					if To_slv ( resize (count - X   ,11,-12))(23) = '1' then
						count <= resize (count + to_sfixed(1,1,0)   ,11,-12);
						output_internal <= resize (output_internal * A,11,-12);
					else
						Done <= '1';
					end if;
				end if;
			end if;
	end process;
	Output <= output_internal;
end RTL;
		
